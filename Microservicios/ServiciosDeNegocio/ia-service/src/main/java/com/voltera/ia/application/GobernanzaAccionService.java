package com.voltera.ia.application;

import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.gobernanza.EstadoAprobaciones;
import com.voltera.ia.domain.model.gobernanza.EstadoAprobaciones.FirmanteEstado;
import com.voltera.ia.domain.model.gobernanza.EstadoPropuesta;
import com.voltera.ia.domain.model.gobernanza.EventoGobernanza;
import com.voltera.ia.domain.model.gobernanza.LecturaOnChain;
import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.in.GobernanzaAccionUseCase;
import com.voltera.ia.domain.port.out.GobernanzaEventPublisherPort;
import com.voltera.ia.domain.port.out.McpBlockchainGatewayPort;
import com.voltera.ia.domain.port.out.NotificadorPort;
import com.voltera.ia.domain.port.out.PropuestaRepositoryPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Orquesta la gobernanza multi-firma 3/3 del documento (Módulo 5). Reúne al
 * Agente de Acción (crea y firma la propuesta), el MCP Blockchain Gateway
 * (registra on-chain), el Agente Notificador (avisa y escala al mismo firmante)
 * y el bus de eventos (tópico {@code der.gobernanza}).
 *
 * <p>Invariante central: la acción NUNCA se ejecuta sin las 3 firmas. La
 * ejecución solo se dispara cuando el estado pasa a APROBADA (3/3) y el gateway
 * confirma. No existe modo degradado 2/3.</p>
 */
public class GobernanzaAccionService implements GobernanzaAccionUseCase {

    private final McpBlockchainGatewayPort gateway;
    private final NotificadorPort notificador;
    private final PropuestaRepositoryPort repositorio;
    private final GobernanzaEventPublisherPort eventos;
    /** TTL de la propuesta según criticidad (del Rules Engine); simplificado aquí. */
    private final Duration ttlCritica;
    private final Duration ttlNormal;

    private final String explorerBase;

    private static final Pattern TX_HASH = Pattern.compile("^0x[0-9a-fA-F]{64}$");
    static final String EXPLORER_POR_DEFECTO = "https://amoy.polygonscan.com";

    public GobernanzaAccionService(McpBlockchainGatewayPort gateway, NotificadorPort notificador,
                                   PropuestaRepositoryPort repositorio, GobernanzaEventPublisherPort eventos,
                                   Duration ttlCritica, Duration ttlNormal) {
        this(gateway, notificador, repositorio, eventos, ttlCritica, ttlNormal, EXPLORER_POR_DEFECTO);
    }

    public GobernanzaAccionService(McpBlockchainGatewayPort gateway, NotificadorPort notificador,
                                   PropuestaRepositoryPort repositorio, GobernanzaEventPublisherPort eventos,
                                   Duration ttlCritica, Duration ttlNormal, String explorerBase) {
        this.explorerBase = explorerBase == null || explorerBase.isBlank()
                ? EXPLORER_POR_DEFECTO : explorerBase.replaceAll("/+$", "");
        this.gateway = gateway;
        this.notificador = notificador;
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.ttlCritica = ttlCritica;
        this.ttlNormal = ttlNormal;
    }

    @Override
    public PropuestaAccion proponer(DiagnosticoDer d) {
        if (!d.requiereAprobacionHumana()) {
            throw new IllegalArgumentException(
                    "El diagnóstico no propone una acción que requiera gobernanza 3/3 (solo monitoreo).");
        }
        Duration ttl = switch (d.severidad()) {
            case CRITICA -> ttlCritica;
            default -> ttlNormal;
        };
        PropuestaAccion p = PropuestaAccion.crear(
                d.diagnosticoId(), d.activoId(), d.comunidadId(), d.accionPropuesta(),
                comandoDesde(d), "severidad=" + d.severidad(), d.severidad(),
                hash(d.diagnostico()), d.promptVersion(), d.motor(), ttl);

        // Firma 1 ya aplicada en crear(). Se registra la propuesta on-chain.
        String tx = gateway.registrarPropuesta(p);
        p.registrarTx(Firmante.AGENTE_IA, tx);       // la creación es la Firma 1
        repositorio.guardar(p);
        eventos.publicar(EventoGobernanza.de("PropuestaAccionCreada", p, tx));

        // El Agente Notificador avisa al siguiente firmante (operador).
        Firmante siguiente = p.siguienteFirmantePendiente();
        if (siguiente != null) {
            notificador.notificarFirmantePendiente(p, siguiente);
        }
        return p;
    }

    @Override
    public PropuestaAccion aprobar(UUID propuestaId, Firmante firmante, String identidad, String razon) {
        PropuestaAccion p = requerir(propuestaId);
        Firma firma = Firma.aprobacion(firmante, identidad, razon);
        p.firmar(firma);
        String tx = gateway.registrarFirma(p, firma);
        p.registrarTx(firmante, tx);
        eventos.publicar(EventoGobernanza.de("FirmaRegistrada", p, tx));
        repositorio.guardar(p);

        switch (p.estado()) {
            case APROBADA -> {
                eventos.publicar(EventoGobernanza.de("AccionAprobada", p, tx));
                ejecutar(p);                       // 3/3 → se dispara la ejecución
            }
            case PENDIENTE -> {
                Firmante siguiente = p.siguienteFirmantePendiente();
                if (siguiente != null) notificador.notificarFirmantePendiente(p, siguiente);
            }
            default -> { /* no debería ocurrir en una aprobación */ }
        }
        return p;
    }

    @Override
    public PropuestaAccion rechazar(UUID propuestaId, Firmante firmante, String identidad, String razon) {
        PropuestaAccion p = requerir(propuestaId);
        Firma firma = Firma.rechazo(firmante, identidad, razon);
        p.firmar(firma);
        String tx = gateway.registrarFirma(p, firma);
        p.registrarTx(firmante, tx);
        repositorio.guardar(p);
        eventos.publicar(EventoGobernanza.de("AccionRechazada", p, tx));
        notificador.notificarDesenlace(p);
        return p;
    }

    @Override
    public List<PropuestaAccion> caducarVencidas() {
        List<PropuestaAccion> vencidas = repositorio.listar().stream()
                .filter(PropuestaAccion::caducarSiVencio)
                .toList();
        vencidas.forEach(p -> {
            repositorio.guardar(p);
            eventos.publicar(EventoGobernanza.de("FirmaVencida", p, "ttl"));
            notificador.notificarDesenlace(p);
        });
        return vencidas;
    }

    @Override
    public Optional<PropuestaAccion> consultar(UUID propuestaId) {
        return repositorio.porId(propuestaId);
    }

    @Override
    public List<PropuestaAccion> listar() {
        return repositorio.listar();
    }

    @Override
    public Optional<EstadoAprobaciones> aprobaciones(UUID propuestaId) {
        return repositorio.porId(propuestaId).map(this::construirAprobaciones);
    }

    // ------------------------------------------------------------------ interno
    private EstadoAprobaciones construirAprobaciones(PropuestaAccion p) {
        LecturaOnChain lectura;
        try {
            lectura = gateway.leerEstado(p);
        } catch (Exception e) {
            lectura = LecturaOnChain.simulada("lectura on-chain no disponible: " + e.getMessage());
        }
        if (lectura == null) lectura = LecturaOnChain.simulada("sin lectura on-chain");

        List<FirmanteEstado> firmantes = new ArrayList<>();
        List<String> faltantes = new ArrayList<>();
        boolean todasReales = true;
        int conTx = 0;
        for (Firmante f : Firmante.values()) {
            Firma firma = p.firmas().stream().filter(x -> x.firmante() == f).findFirst().orElse(null);
            if (firma == null) {
                firmantes.add(new FirmanteEstado(f.name(), EstadoAprobaciones.PENDIENTE,
                        null, null, null, null, null, LecturaOnChain.SIMULADO));
                if (p.estado() == EstadoPropuesta.PENDIENTE) faltantes.add(f.name());
                continue;
            }
            String tx = p.txDe(f);
            Instant cuando = firma.firmadoEn();
            if (lectura.esOnChain() && lectura.firmas() != null) {
                for (LecturaOnChain.FirmaOnChain fo : lectura.firmas()) {
                    if (f.name().equals(fo.rol()) && fo.txHash() != null && esHash(fo.txHash())) {
                        tx = fo.txHash();
                        if (fo.firmadoEn() != null) cuando = fo.firmadoEn();
                    }
                }
            }
            boolean real = esHash(tx);
            if (tx != null) { conTx++; if (!real) todasReales = false; }
            firmantes.add(new FirmanteEstado(f.name(),
                    firma.aprueba() ? EstadoAprobaciones.FIRMO : EstadoAprobaciones.RECHAZO,
                    firma.identidad(), firma.razon(), cuando, tx,
                    real ? urlTx(tx) : null, real ? LecturaOnChain.ON_CHAIN : LecturaOnChain.SIMULADO));
        }
        boolean onChain = (lectura.esOnChain() && lectura.existe()) || (conTx > 0 && todasReales);
        String txEj = p.txEjecucion();
        return new EstadoAprobaciones(
                p.propuestaId().toString(), p.activoId(), p.comando(),
                p.estado().name(), etiquetaGlobal(p), 3, p.firmasAprobadas(), p.firmasResumen(),
                onChain ? LecturaOnChain.ON_CHAIN : LecturaOnChain.SIMULADO,
                lectura.red(), lectura.contrato(), explorerBase, p.creadaEn(), p.venceEn(),
                firmantes, faltantes, txEj, esHash(txEj) ? urlTx(txEj) : null, lectura);
    }

    private String etiquetaGlobal(PropuestaAccion p) {
        return switch (p.estado()) {
            case PENDIENTE -> "PENDIENTE (" + p.firmasResumen() + "): faltan firmas, no se ejecuta";
            case APROBADA -> "APROBADA 3-de-3";
            case EJECUTADA -> "APROBADA 3-de-3 y EJECUTADA";
            case RECHAZADA -> "RECHAZADA: un firmante rechazó, no se ejecuta";
            case VENCIDA -> "VENCIDA: TTL expirado sin 3/3, no se ejecuta";
        };
    }

    private boolean esHash(String tx) {
        return tx != null && TX_HASH.matcher(tx).matches();
    }

    private String urlTx(String tx) {
        return explorerBase + "/tx/" + tx;
    }


    private void ejecutar(PropuestaAccion p) {
        String tx = gateway.ejecutarAccion(p);   // el gateway solo ejecuta si está 3/3
        p.registrarTxEjecucion(tx);
        p.marcarEjecutada("comando emitido vía MCP Device Management · " + tx);
        repositorio.guardar(p);
        eventos.publicar(EventoGobernanza.de("AccionEjecutada", p, tx));
        notificador.notificarDesenlace(p);
    }

    private PropuestaAccion requerir(UUID id) {
        return repositorio.porId(id).orElseThrow(
                () -> new IllegalArgumentException("Propuesta no encontrada: " + id));
    }

    private String comandoDesde(DiagnosticoDer d) {
        return switch (d.accionPropuesta()) {
            case COMANDO_FISICO -> "EJECUTAR_COMANDO_FISICO(" + d.activoId() + ")";
            case CORTAR_SUMINISTRO -> "CORTAR_SUMINISTRO(" + d.activoId() + ")";
            case DESPACHAR_TECNICO -> "DESPACHAR_TECNICO(" + d.activoId() + ")";
            case MONITOREAR -> "MONITOREAR(" + d.activoId() + ")";
        };
    }

    private String hash(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest((texto == null ? "" : texto).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("0x");
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "0x0";
        }
    }
}
