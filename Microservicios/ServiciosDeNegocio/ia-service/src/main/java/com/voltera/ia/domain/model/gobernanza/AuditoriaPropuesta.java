package com.voltera.ia.domain.model.gobernanza;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Línea de tiempo de auditoría de una propuesta (insumo para la CREG): quién hizo
 * qué y cuándo, con la referencia de transacción. Se reconstruye del agregado, sin
 * almacenamiento adicional.
 */
public record AuditoriaPropuesta(
        String propuestaId,
        String estado,
        String hashDiagnostico,
        String versionPrompt,
        String versionModelo,
        List<Evento> eventos
) {
    /** Hito de la auditoría. {@code tx} puede ser un hash real o una referencia {@code offchain:}. */
    public record Evento(Instant instante, String tipo, String actor, String detalle, String tx) {}

    public static AuditoriaPropuesta desde(PropuestaAccion p) {
        List<Evento> ev = new ArrayList<>();
        for (Firma f : p.firmas()) {
            String tipo = f.aprueba() ? "FIRMA_" + f.firmante() : "RECHAZO_" + f.firmante();
            String tx = f.firmante() == Firmante.AGENTE_IA ? null : p.txDe(f.firmante());
            ev.add(new Evento(f.firmadoEn(), tipo, f.identidad(), f.razon(), tx));
        }
        if (p.estado() == EstadoPropuesta.APROBADA || p.estado() == EstadoPropuesta.EJECUTADA) {
            ev.add(new Evento(ultimaFirma(p), "APROBADA_3_DE_3", "contrato", "Las tres firmas están registradas", null));
        }
        if (p.estado() == EstadoPropuesta.EJECUTADA) {
            ev.add(new Evento(ultimaFirma(p), "EJECUTADA", "agente-accion@voltera",
                    p.resultadoEjecucion() == null ? "" : p.resultadoEjecucion(), p.txEjecucion()));
        }
        if (p.estado() == EstadoPropuesta.VENCIDA) {
            ev.add(new Evento(p.venceEn(), "VENCIDA", "contrato", "TTL expirado sin 3/3; no se ejecuta", null));
        }
        ev.sort(Comparator.comparing(Evento::instante));
        ev.add(0, new Evento(p.creadaEn(), "PROPUESTA_CREADA", "agente-accion@voltera",
                p.comando() + " sobre " + p.activoId() + " (vence " + p.venceEn() + ")", p.txDe(Firmante.AGENTE_IA)));
        return new AuditoriaPropuesta(p.propuestaId().toString(), p.estado().name(),
                p.hashDiagnostico(), p.versionPrompt(), p.versionModelo(), List.copyOf(ev));
    }

    private static Instant ultimaFirma(PropuestaAccion p) {
        return p.firmas().stream().map(Firma::firmadoEn).max(Comparator.naturalOrder()).orElse(p.creadaEn());
    }
}
