package com.voltera.ia.application;

import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.gobernanza.EventoGobernanza;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public GobernanzaAccionService(McpBlockchainGatewayPort gateway, NotificadorPort notificador,
                                   PropuestaRepositoryPort repositorio, GobernanzaEventPublisherPort eventos,
                                   Duration ttlCritica, Duration ttlNormal) {
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

    // ------------------------------------------------------------------ interno

    private void ejecutar(PropuestaAccion p) {
        String tx = gateway.ejecutarAccion(p);   // el gateway solo ejecuta si está 3/3
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
