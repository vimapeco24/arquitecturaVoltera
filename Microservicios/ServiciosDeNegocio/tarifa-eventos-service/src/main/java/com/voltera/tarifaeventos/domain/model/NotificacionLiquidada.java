package com.voltera.tarifaeventos.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Notificacion liquidada que se genera al activar un nuevo medidor/prosumidor.
 *
 * <p>Modela el requisito de negocio: "al activar un nuevo medidor y/o prosumidor
 * se debe generar notificacion con factura, tarifas, etc. (liquidados)". La
 * notificacion resume las condiciones tarifarias vigentes (cargo fijo, precio
 * base del kWh, umbral contratado y precio del kWh extra) para que el prosumidor
 * conozca de entrada como se le facturara.</p>
 *
 * <p>Este objeto se publica en un <b>topico de notificacion</b> para que otros
 * servicios (correo, app movil, portal del prosumidor) reaccionen.</p>
 */
public record NotificacionLiquidada(
        String notificacionId,
        String medidorId,
        String prosumidorId,
        String tipo,
        BigDecimal cargoFijoMensual,
        BigDecimal precioBaseKwh,
        BigDecimal umbralKwh,
        BigDecimal precioExtraKwh,
        String mensaje,
        Instant generadaEn
) {

    /** Cargo fijo mensual por disponibilidad del servicio (COP). */
    public static final BigDecimal CARGO_FIJO_MENSUAL = new BigDecimal("18000.00");
    /** Precio base del kWh dentro del umbral (COP). */
    public static final BigDecimal PRECIO_BASE_KWH = new BigDecimal("620.00");

    public static NotificacionLiquidada porActivacion(String medidorId, String prosumidorId,
                                                      BigDecimal umbralKwh) {
        String mensaje = String.format(
                "Medidor %s activado para el prosumidor %s. Factura liquidada: cargo fijo %s COP/mes, " +
                "precio base %s COP/kWh hasta %s kWh, y %s COP/kWh por consumo extra.",
                medidorId, prosumidorId, CARGO_FIJO_MENSUAL.toPlainString(),
                PRECIO_BASE_KWH.toPlainString(), umbralKwh.toPlainString(),
                CargoTarifa.PRECIO_EXTRA_KWH.toPlainString());

        return new NotificacionLiquidada(
                "NOT-" + UUID.randomUUID(),
                medidorId,
                prosumidorId,
                "MEDIDOR_ACTIVADO",
                CARGO_FIJO_MENSUAL,
                PRECIO_BASE_KWH,
                umbralKwh,
                CargoTarifa.PRECIO_EXTRA_KWH,
                mensaje,
                Instant.now()
        );
    }
}
