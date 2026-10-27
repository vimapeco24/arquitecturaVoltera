package com.voltera.certificacion.domain.model;

import java.time.Instant;

/**
 * Agregado (lámina 04): una solicitud para certificar un bloque de generación
 * renovable ya reservado. Reúne los DOS avales requeridos — el de <b>Voltera</b>
 * (operador) y el de la <b>certificadora externa</b> — antes de quedar APROBADA.
 *
 * <p>Estados: PENDIENTE → (avalVoltera + avalCertificadora) → APROBADA; o
 * RECHAZADA. Solo cuando está APROBADA el Servicio de Certificados Renovables
 * podrá emitir el NFT (vía gateway). Este agregado NO toca la cadena.</p>
 */
public class SolicitudCertificacion {

    public enum Estado { PENDIENTE, APROBADA, RECHAZADA }

    private final String id;
    private final String medidorId;
    private final String propietario;
    private final long energiaWh;        // energía del bloque a certificar (= bloqueWh)
    private final int periodo;           // AAAAMM
    private Estado estado = Estado.PENDIENTE;
    private boolean avalVoltera;
    private boolean avalCertificadora;
    private String motivoRechazo;
    private final Instant creadaEn = Instant.now();
    private Instant resueltaEn;

    public SolicitudCertificacion(String id, String medidorId, String propietario,
                                  long energiaWh, int periodo) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id requerido");
        if (medidorId == null || medidorId.isBlank()) throw new IllegalArgumentException("medidorId requerido");
        if (propietario == null || propietario.isBlank()) throw new IllegalArgumentException("propietario requerido");
        if (energiaWh <= 0) throw new IllegalArgumentException("energiaWh debe ser > 0");
        if (periodo <= 0) throw new IllegalArgumentException("periodo (AAAAMM) debe ser > 0");
        this.id = id;
        this.medidorId = medidorId;
        this.propietario = propietario;
        this.energiaWh = energiaWh;
        this.periodo = periodo;
    }

    /** Aval del operador Voltera. Al tener ambos avales, la solicitud se aprueba. */
    public void avalarVoltera() {
        exigirPendiente();
        this.avalVoltera = true;
        intentarAprobar();
    }

    /** Aval de la certificadora externa. Al tener ambos avales, se aprueba. */
    public void avalarCertificadora() {
        exigirPendiente();
        this.avalCertificadora = true;
        intentarAprobar();
    }

    public void rechazar(String motivo) {
        exigirPendiente();
        this.estado = Estado.RECHAZADA;
        this.motivoRechazo = motivo;
        this.resueltaEn = Instant.now();
    }

    private void intentarAprobar() {
        if (avalVoltera && avalCertificadora) {
            this.estado = Estado.APROBADA;
            this.resueltaEn = Instant.now();
        }
    }

    private void exigirPendiente() {
        if (estado != Estado.PENDIENTE) {
            throw new IllegalStateException("La solicitud " + id + " ya está " + estado);
        }
    }

    public boolean estaAprobada() { return estado == Estado.APROBADA; }

    public String id() { return id; }
    public String medidorId() { return medidorId; }
    public String propietario() { return propietario; }
    public long energiaWh() { return energiaWh; }
    public int periodo() { return periodo; }
    public Estado estado() { return estado; }
    public boolean avalVoltera() { return avalVoltera; }
    public boolean avalCertificadora() { return avalCertificadora; }
    public String motivoRechazo() { return motivoRechazo; }
    public Instant creadaEn() { return creadaEn; }
    public Instant resueltaEn() { return resueltaEn; }
}
