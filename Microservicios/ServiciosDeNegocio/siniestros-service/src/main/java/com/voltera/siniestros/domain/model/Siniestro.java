package com.voltera.siniestros.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad Raiz («root») del agregado Siniestro.
 * Modela una reclamacion por siniestro con maquina de estados:
 * REPORTADO -> EN_PERITAJE -> (APROBADO | RECHAZADO).
 */
public class Siniestro {

    private final SiniestroId id;
    private final String polizaId;
    private final String prosumidorId;
    private final String descripcion;
    private final MontoReclamacion montoReclamacion;
    private EstadoSiniestro estado;
    private final LocalDate fechaOcurrencia;
    private final Instant reportadoEn;

    private Siniestro(SiniestroId id, String polizaId, String prosumidorId, String descripcion,
                      MontoReclamacion montoReclamacion, EstadoSiniestro estado,
                      LocalDate fechaOcurrencia, Instant reportadoEn) {
        this.id = id;
        this.polizaId = polizaId;
        this.prosumidorId = prosumidorId;
        this.descripcion = descripcion;
        this.montoReclamacion = montoReclamacion;
        this.estado = estado;
        this.fechaOcurrencia = fechaOcurrencia;
        this.reportadoEn = reportadoEn;
    }

    public static Siniestro reportar(String polizaId, String prosumidorId, String descripcion,
                                     MontoReclamacion montoReclamacion, LocalDate fechaOcurrencia) {
        validar(polizaId, prosumidorId, descripcion, montoReclamacion, fechaOcurrencia);
        return new Siniestro(SiniestroId.nuevo(), polizaId, prosumidorId, descripcion,
                montoReclamacion, EstadoSiniestro.REPORTADO, fechaOcurrencia, Instant.now());
    }

    public static Siniestro reconstituir(SiniestroId id, String polizaId, String prosumidorId,
                                         String descripcion, MontoReclamacion montoReclamacion,
                                         EstadoSiniestro estado, LocalDate fechaOcurrencia,
                                         Instant reportadoEn) {
        return new Siniestro(id, polizaId, prosumidorId, descripcion, montoReclamacion,
                estado, fechaOcurrencia, reportadoEn);
    }

    private static void validar(String polizaId, String prosumidorId, String descripcion,
                                MontoReclamacion montoReclamacion, LocalDate fechaOcurrencia) {
        if (polizaId == null || polizaId.isBlank()) {
            throw new IllegalArgumentException("polizaId requerido");
        }
        if (prosumidorId == null || prosumidorId.isBlank()) {
            throw new IllegalArgumentException("prosumidorId requerido");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("descripcion requerida");
        }
        Objects.requireNonNull(montoReclamacion, "montoReclamacion requerido");
        Objects.requireNonNull(fechaOcurrencia, "fechaOcurrencia requerida");
    }

    /** Invariante: solo un siniestro REPORTADO puede pasar a EN_PERITAJE. */
    public void enviarAPeritaje() {
        if (estado != EstadoSiniestro.REPORTADO) {
            throw new IllegalStateException("Solo un siniestro REPORTADO puede pasar a EN_PERITAJE");
        }
        this.estado = EstadoSiniestro.EN_PERITAJE;
    }

    /**
     * Invariante: solo un siniestro EN_PERITAJE puede APROBARSE.
     * Al aprobarse se genera el evento de dominio {@link SiniestroAprobado}.
     */
    public SiniestroAprobado aprobar() {
        if (estado != EstadoSiniestro.EN_PERITAJE) {
            throw new IllegalStateException("Solo un siniestro EN_PERITAJE puede APROBARSE");
        }
        this.estado = EstadoSiniestro.APROBADO;
        return SiniestroAprobado.desde(this);
    }

    /** Invariante: solo un siniestro EN_PERITAJE puede RECHAZARSE. */
    public void rechazar() {
        if (estado != EstadoSiniestro.EN_PERITAJE) {
            throw new IllegalStateException("Solo un siniestro EN_PERITAJE puede RECHAZARSE");
        }
        this.estado = EstadoSiniestro.RECHAZADO;
    }

    public SiniestroId id() { return id; }
    public String polizaId() { return polizaId; }
    public String prosumidorId() { return prosumidorId; }
    public String descripcion() { return descripcion; }
    public MontoReclamacion montoReclamacion() { return montoReclamacion; }
    public EstadoSiniestro estado() { return estado; }
    public LocalDate fechaOcurrencia() { return fechaOcurrencia; }
    public Instant reportadoEn() { return reportadoEn; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Siniestro s)) return false;
        return id.equals(s.id);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
