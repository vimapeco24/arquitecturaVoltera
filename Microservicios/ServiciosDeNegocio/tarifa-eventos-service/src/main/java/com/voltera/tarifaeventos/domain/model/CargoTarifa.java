package com.voltera.tarifaeventos.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Agregado raiz del modelo de ESCRITURA (CQRS - write side).
 *
 * <p>Representa el cargo tarifario que el proveedor calcula cuando el medidor de
 * un prosumidor reporta un consumo. Se construye por <b>transferencia de
 * estado</b> a partir del evento {@code ConsumoRegistrado}: usa los datos que
 * viajan en el evento (consumoKwh, umbralKwh, si es consumo extra) y NO consulta
 * a telemetria-service.</p>
 *
 * <p>Regla de negocio: si la lectura supero el umbral contratado, se cobra el
 * excedente {@code (consumoKwh - umbralKwh) * PRECIO_EXTRA_KWH} y el cargo queda
 * LIQUIDADO; si no, el cargo es SIN_CARGO (monto 0).</p>
 *
 * <p>La clave de idempotencia es {@code eventId}.</p>
 */
public class CargoTarifa {

    /** Precio del kWh por consumo extra (excedente sobre el umbral), en COP. */
    public static final BigDecimal PRECIO_EXTRA_KWH = new BigDecimal("850.00");

    private final CargoId id;
    private final String eventId;
    private final String medidorId;
    private final String prosumidorId;
    private final BigDecimal consumoKwh;
    private final BigDecimal umbralKwh;
    private final BigDecimal excedenteKwh;
    private final BigDecimal precioExtraKwh;
    private final BigDecimal montoCargo;
    private final EstadoCargo estado;
    private final Instant creadoEn;

    private CargoTarifa(CargoId id, String eventId, String medidorId, String prosumidorId,
                        BigDecimal consumoKwh, BigDecimal umbralKwh, BigDecimal excedenteKwh,
                        BigDecimal precioExtraKwh, BigDecimal montoCargo, EstadoCargo estado,
                        Instant creadoEn) {
        this.id = id;
        this.eventId = eventId;
        this.medidorId = medidorId;
        this.prosumidorId = prosumidorId;
        this.consumoKwh = consumoKwh;
        this.umbralKwh = umbralKwh;
        this.excedenteKwh = excedenteKwh;
        this.precioExtraKwh = precioExtraKwh;
        this.montoCargo = montoCargo;
        this.estado = estado;
        this.creadoEn = creadoEn;
    }

    /**
     * Factory: crea el cargo a partir de los datos del evento (transferencia de
     * estado). Calcula el excedente y el monto solo si hubo consumo extra.
     */
    public static CargoTarifa crearDesdeEvento(String eventId,
                                               String medidorId,
                                               String prosumidorId,
                                               BigDecimal consumoKwh,
                                               BigDecimal umbralKwh,
                                               boolean consumoExtra) {
        Objects.requireNonNull(eventId, "eventId es obligatorio");
        Objects.requireNonNull(consumoKwh, "consumoKwh es obligatorio");
        Objects.requireNonNull(umbralKwh, "umbralKwh es obligatorio");

        BigDecimal excedente = BigDecimal.ZERO;
        BigDecimal monto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        EstadoCargo estado = EstadoCargo.SIN_CARGO;

        if (consumoExtra) {
            excedente = consumoKwh.subtract(umbralKwh).max(BigDecimal.ZERO);
            monto = excedente.multiply(PRECIO_EXTRA_KWH).setScale(2, RoundingMode.HALF_UP);
            estado = EstadoCargo.LIQUIDADO;
        }

        return new CargoTarifa(
                CargoId.nuevo(),
                eventId,
                medidorId,
                prosumidorId,
                consumoKwh,
                umbralKwh,
                excedente,
                PRECIO_EXTRA_KWH,
                monto,
                estado,
                Instant.now()
        );
    }

    public static CargoTarifa reconstituir(CargoId id, String eventId, String medidorId,
                                           String prosumidorId, BigDecimal consumoKwh,
                                           BigDecimal umbralKwh, BigDecimal excedenteKwh,
                                           BigDecimal precioExtraKwh, BigDecimal montoCargo,
                                           EstadoCargo estado, Instant creadoEn) {
        return new CargoTarifa(id, eventId, medidorId, prosumidorId, consumoKwh, umbralKwh,
                excedenteKwh, precioExtraKwh, montoCargo, estado, creadoEn);
    }

    public CargoId getId() { return id; }
    public String getEventId() { return eventId; }
    public String getMedidorId() { return medidorId; }
    public String getProsumidorId() { return prosumidorId; }
    public BigDecimal getConsumoKwh() { return consumoKwh; }
    public BigDecimal getUmbralKwh() { return umbralKwh; }
    public BigDecimal getExcedenteKwh() { return excedenteKwh; }
    public BigDecimal getPrecioExtraKwh() { return precioExtraKwh; }
    public BigDecimal getMontoCargo() { return montoCargo; }
    public EstadoCargo getEstado() { return estado; }
    public Instant getCreadoEn() { return creadoEn; }
}
