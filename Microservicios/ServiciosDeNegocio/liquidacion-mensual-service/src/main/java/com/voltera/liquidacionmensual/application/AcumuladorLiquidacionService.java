package com.voltera.liquidacionmensual.application;

import com.voltera.liquidacionmensual.domain.model.MensajeOutbox;
import com.voltera.liquidacionmensual.domain.port.in.AcumularConsumoUseCase;
import com.voltera.liquidacionmensual.domain.port.out.OutboxPort;
import com.voltera.liquidacionmensual.domain.port.out.SerializadorEventosPort;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de aplicacion EDA de Liquidacion Mensual (diagrama DDD 02).
 *
 * <p>Consume el consumo por intervalo (origen: ConsumoIntervaloRegistrado de
 * Telemetria) y lo acumula por medidor. Al superar el umbral de cierre emite
 * {@code LiquidacionCalculada} por el OUTBOX (destino: Facturacion). Es un
 * complemento EDA que no altera el LiquidacionMensualService REST existente.</p>
 */
public class AcumuladorLiquidacionService implements AcumularConsumoUseCase {

    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;
    private final double umbralCierreKwh;

    /** Acumulado de kWh por medidor en el periodo en curso. */
    private final ConcurrentHashMap<String, Double> acumuladoPorMedidor = new ConcurrentHashMap<>();

    public AcumuladorLiquidacionService(OutboxPort outbox, SerializadorEventosPort serializador,
                                        double umbralCierreKwh) {
        this.outbox = outbox;
        this.serializador = serializador;
        this.umbralCierreKwh = umbralCierreKwh;
    }

    @Override
    public void acumular(String medidorSerial, double consumoNetoKwh) {
        if (medidorSerial == null || medidorSerial.isBlank()) return;
        double acumulado = acumuladoPorMedidor.merge(medidorSerial, consumoNetoKwh, Double::sum);
        if (acumulado >= umbralCierreKwh) {
            emitirLiquidacion(medidorSerial, acumulado);
            acumuladoPorMedidor.put(medidorSerial, 0.0); // reinicia el periodo
        }
    }

    private void emitirLiquidacion(String medidorSerial, double totalKwh) {
        String periodo = YearMonth.now(ZoneOffset.UTC).toString(); // ej 2026-10
        String liquidacionId = "LIQ-" + medidorSerial + "-" + periodo;

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("liquidacionId", liquidacionId);
        payload.put("medidorSerial", medidorSerial);
        payload.put("periodo", periodo);
        payload.put("consumoTotalKwh", String.valueOf(totalKwh));
        payload.put("calculadaEn", Instant.now().toString());
        outbox.agregar(MensajeOutbox.pendiente("LiquidacionCalculada", medidorSerial, serializador.aJson(payload)));
    }

    /** Consulta auxiliar para la API REST de depuracion. */
    public double acumuladoDe(String medidorSerial) {
        return acumuladoPorMedidor.getOrDefault(medidorSerial, 0.0);
    }
}
