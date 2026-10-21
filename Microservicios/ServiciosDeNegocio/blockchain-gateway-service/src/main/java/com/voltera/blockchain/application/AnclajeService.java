package com.voltera.blockchain.application;

import com.voltera.blockchain.domain.exception.AnclajeNoEncontradoException;
import com.voltera.blockchain.domain.model.Anclaje;
import com.voltera.blockchain.domain.model.LiquidacionAnclada;
import com.voltera.blockchain.domain.model.LiquidacionP2P;
import com.voltera.blockchain.domain.port.in.AnclarLiquidacionUseCase;
import com.voltera.blockchain.domain.port.in.ConsultarAnclajeUseCase;
import com.voltera.blockchain.domain.port.out.AnclajeRepositoryPort;
import com.voltera.blockchain.domain.port.out.CadenaBlockchainPort;
import com.voltera.blockchain.domain.port.out.EventPublisherPort;
import com.voltera.blockchain.domain.port.out.HashServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de aplicacion del gateway. Orquesta el patron OUTBOX:
 *  1) anclar(): computa el hash y persiste el Anclaje PENDIENTE (no bloquea en la cadena).
 *  2) procesarPendientes(): el relay (programado) lleva los PENDIENTE a la cadena;
 *     al confirmar, publica LiquidacionAnclada en Kafka (confirmacion por evento, gate 6).
 *
 * Agnostico de framework (sin anotaciones Spring): el wiring vive en config.
 */
public class AnclajeService implements AnclarLiquidacionUseCase, ConsultarAnclajeUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnclajeService.class);
    private static final int MAX_INTENTOS = 10;

    private final AnclajeRepositoryPort repositorio;
    private final CadenaBlockchainPort cadena;
    private final EventPublisherPort publicador;
    private final HashServicePort hashService;
    private final String red;

    public AnclajeService(AnclajeRepositoryPort repositorio, CadenaBlockchainPort cadena,
                          EventPublisherPort publicador, HashServicePort hashService, String red) {
        this.repositorio = repositorio;
        this.cadena = cadena;
        this.publicador = publicador;
        this.hashService = hashService;
        this.red = red;
    }

    @Override
    public Anclaje anclar(LiquidacionP2P liquidacion) {
        // Idempotencia: si ya existe un anclaje para esta transaccion, devolverlo.
        var existente = repositorio.buscarPorTransaccion(liquidacion.transaccionId());
        if (existente.isPresent()) {
            log.info("Anclaje ya existente para transaccion={} estado={}",
                    liquidacion.transaccionId(), existente.get().estado());
            return existente.get();
        }

        String hash = hashService.keccak256Hex(liquidacion);
        Anclaje anclaje = Anclaje.pendiente(
                liquidacion.transaccionId(), hash,
                liquidacion.energiaWh(), liquidacion.liquidadaEn().getEpochSecond(),
                liquidacion.comunidadId());
        Anclaje guardado = repositorio.guardar(anclaje);
        log.info("Liquidacion registrada en OUTBOX transaccion={} hash={} (PENDIENTE)",
                guardado.transaccionId(), guardado.hashCasacion());

        // Intento inmediato best-effort; si falla, el relay reintenta.
        procesar(guardado);
        return repositorio.buscarPorTransaccion(liquidacion.transaccionId()).orElse(guardado);
    }

    /** Relay del OUTBOX: procesa todos los anclajes PENDIENTE. */
    public void procesarPendientes() {
        List<Anclaje> pendientes = repositorio.buscarPendientes();
        if (pendientes.isEmpty()) return;
        if (!cadena.disponible()) {
            log.warn("RPC del tercero no disponible; se reintentara. pendientes={}", pendientes.size());
            return;
        }
        log.info("Procesando {} anclaje(s) pendiente(s)", pendientes.size());
        for (Anclaje a : pendientes) {
            procesar(a);
        }
    }

    private void procesar(Anclaje anclaje) {
        if (!anclaje.estaPendiente()) return;

        CadenaBlockchainPort.ResultadoAnclaje r = cadena.anclar(anclaje);
        if (r.confirmado()) {
            anclaje.marcarConfirmado(r.txHash(), r.bloque(), r.gasUsado());
            repositorio.guardar(anclaje);
            log.info("Anclaje CONFIRMADO transaccion={} txHash={} bloque={} gas={}",
                    anclaje.transaccionId(), r.txHash(), r.bloque(), r.gasUsado());
            publicarConfirmacion(anclaje);
        } else {
            boolean definitivo = r.definitivo() || anclaje.intentos() + 1 >= MAX_INTENTOS;
            anclaje.registrarIntentoFallido(r.error(), definitivo);
            repositorio.guardar(anclaje);
            log.warn("Anclaje no confirmado transaccion={} intento={} definitivo={} error={}",
                    anclaje.transaccionId(), anclaje.intentos(), definitivo, r.error());
        }
    }

    private void publicarConfirmacion(Anclaje anclaje) {
        LiquidacionAnclada evento = LiquidacionAnclada.de(anclaje, red);
        boolean ok = publicador.publicar(evento);
        if (!ok) {
            log.warn("Evento LiquidacionAnclada no confirmado por el broker transaccion={} "
                    + "(se reintentara al reprocesar)", anclaje.transaccionId());
        }
    }

    @Override
    public Anclaje porTransaccion(String transaccionId) {
        return repositorio.buscarPorTransaccion(transaccionId)
                .orElseThrow(() -> new AnclajeNoEncontradoException(transaccionId));
    }

    @Override
    public List<Anclaje> todos() {
        return repositorio.buscarTodos();
    }

    @Override
    public List<ResumenComunidad> resumenComunidades() {
        Map<String, List<Anclaje>> porComunidad = new LinkedHashMap<>();
        for (Anclaje a : repositorio.buscarTodos()) {
            String clave = a.comunidadId() == null ? "(sin comunidad)" : a.comunidadId();
            porComunidad.computeIfAbsent(clave, k -> new ArrayList<>()).add(a);
        }
        List<ResumenComunidad> resumen = new ArrayList<>();
        for (var e : porComunidad.entrySet()) {
            List<Anclaje> lista = e.getValue();
            long confirmados = lista.stream().filter(Anclaje::estaConfirmado).count();
            long pendientes = lista.stream().filter(Anclaje::estaPendiente).count();
            long energia = lista.stream().mapToLong(Anclaje::energiaWh).sum();
            resumen.add(new ResumenComunidad(e.getKey(), lista.size(), confirmados, pendientes, energia));
        }
        return resumen;
    }

    @Override
    public ResultadoVerificacion verificar(LiquidacionP2P liquidacion) {
        String hashRecomputado = hashService.keccak256Hex(liquidacion);
        var existente = repositorio.buscarPorTransaccion(liquidacion.transaccionId());
        if (existente.isEmpty()) {
            return new ResultadoVerificacion(false, false, hashRecomputado, null, null, "SIN_ANCLAR");
        }
        Anclaje a = existente.get();
        boolean integro = a.hashCasacion() != null
                && a.hashCasacion().equalsIgnoreCase(hashRecomputado);
        return new ResultadoVerificacion(
                true, integro, hashRecomputado, a.hashCasacion(), a.txHash(), a.estado().name());
    }
}
