package com.voltera.reaseguro.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Agregado raiz del modelo de ESCRITURA (CQRS - write side).
 *
 * <p>Representa la cesion de una parte del riesgo de un siniestro aprobado hacia
 * la reaseguradora. Se construye por transferencia de estado a partir del evento
 * {@code SiniestroAprobado}: no consulta a otros servicios, usa los datos que
 * viajan en el evento.</p>
 *
 * <p>La clave de idempotencia es {@code eventId}: dos eventos con el mismo id no
 * deben generar dos cesiones.</p>
 */
public class CesionRiesgo {

    /** Porcentaje por defecto que se cede a la reaseguradora (40%). */
    public static final BigDecimal PORCENTAJE_CEDIDO_DEFAULT = new BigDecimal("40");

    private final CesionId id;
    private final String eventId;
    private final String siniestroId;
    private final String polizaId;
    private final String prosumidorId;
    private final BigDecimal montoAprobado;
    private final BigDecimal porcentajeCedido;
    private final BigDecimal montoCedido;
    private EstadoCesion estado;
    private final Instant creadaEn;

    private CesionRiesgo(CesionId id,
                         String eventId,
                         String siniestroId,
                         String polizaId,
                         String prosumidorId,
                         BigDecimal montoAprobado,
                         BigDecimal porcentajeCedido,
                         BigDecimal montoCedido,
                         EstadoCesion estado,
                         Instant creadaEn) {
        this.id = id;
        this.eventId = eventId;
        this.siniestroId = siniestroId;
        this.polizaId = polizaId;
        this.prosumidorId = prosumidorId;
        this.montoAprobado = montoAprobado;
        this.porcentajeCedido = porcentajeCedido;
        this.montoCedido = montoCedido;
        this.estado = estado;
        this.creadaEn = creadaEn;
    }

    /**
     * Factory: crea una cesion a partir de los datos que trae el evento
     * (transferencia de estado). Aplica el porcentaje por defecto (40%) y calcula
     * {@code montoCedido = montoAprobado * porcentajeCedido / 100}.
     */
    public static CesionRiesgo crearDesdeEvento(String eventId,
                                                String siniestroId,
                                                String polizaId,
                                                String prosumidorId,
                                                BigDecimal montoAprobado) {
        Objects.requireNonNull(eventId, "eventId es obligatorio");
        Objects.requireNonNull(montoAprobado, "montoAprobado es obligatorio");
        if (montoAprobado.signum() < 0) {
            throw new IllegalArgumentException("El monto aprobado no puede ser negativo");
        }

        BigDecimal porcentaje = PORCENTAJE_CEDIDO_DEFAULT;
        BigDecimal montoCedido = montoAprobado
                .multiply(porcentaje)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return new CesionRiesgo(
                CesionId.nuevo(),
                eventId,
                siniestroId,
                polizaId,
                prosumidorId,
                montoAprobado,
                porcentaje,
                montoCedido,
                EstadoCesion.PENDIENTE,
                Instant.now()
        );
    }

    /**
     * Reconstruye una cesion existente (por ejemplo desde persistencia).
     */
    public static CesionRiesgo reconstituir(CesionId id,
                                            String eventId,
                                            String siniestroId,
                                            String polizaId,
                                            String prosumidorId,
                                            BigDecimal montoAprobado,
                                            BigDecimal porcentajeCedido,
                                            BigDecimal montoCedido,
                                            EstadoCesion estado,
                                            Instant creadaEn) {
        return new CesionRiesgo(id, eventId, siniestroId, polizaId, prosumidorId,
                montoAprobado, porcentajeCedido, montoCedido, estado, creadaEn);
    }

    /**
     * Transicion de estado PENDIENTE -> CEDIDA (aprobacion por la reaseguradora).
     */
    public void confirmarCesion() {
        if (this.estado != EstadoCesion.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se puede confirmar una cesion en estado PENDIENTE, estado actual: " + this.estado);
        }
        this.estado = EstadoCesion.CEDIDA;
    }

    public CesionId getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSiniestroId() {
        return siniestroId;
    }

    public String getPolizaId() {
        return polizaId;
    }

    public String getProsumidorId() {
        return prosumidorId;
    }

    public BigDecimal getMontoAprobado() {
        return montoAprobado;
    }

    public BigDecimal getPorcentajeCedido() {
        return porcentajeCedido;
    }

    public BigDecimal getMontoCedido() {
        return montoCedido;
    }

    public EstadoCesion getEstado() {
        return estado;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }
}
