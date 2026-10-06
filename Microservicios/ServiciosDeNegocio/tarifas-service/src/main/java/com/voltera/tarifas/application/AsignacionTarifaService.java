package com.voltera.tarifas.application;

import com.voltera.tarifas.domain.model.FranjaHoraria;
import com.voltera.tarifas.domain.model.MensajeOutbox;
import com.voltera.tarifas.domain.model.Precio;
import com.voltera.tarifas.domain.model.Tarifa;
import com.voltera.tarifas.domain.model.Vigencia;
import com.voltera.tarifas.domain.port.in.AsignarTarifaUseCase;
import com.voltera.tarifas.domain.port.out.OutboxPort;
import com.voltera.tarifas.domain.port.out.SerializadorEventosPort;
import com.voltera.tarifas.domain.port.out.TarifaRepositoryPort;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de aplicacion EDA del BC Tarifas (diagrama DDD 02). Reacciona a eventos:
 * <ul>
 *   <li>{@code MedidorHabilitado} -> asigna una {@link Tarifa} vigente al medidor y
 *       emite {@code TarifaAsignada} (con Vigencia) por el OUTBOX.</li>
 *   <li>{@code ConsumoIntervaloRegistrado} -> tarifica el consumo del intervalo con
 *       el precio vigente de la tarifa.</li>
 * </ul>
 * No altera el {@link TarifaService} REST existente: lo complementa.
 */
public class AsignacionTarifaService implements AsignarTarifaUseCase {

    private final TarifaRepositoryPort repositorio;
    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;

    public AsignacionTarifaService(TarifaRepositoryPort repositorio, OutboxPort outbox,
                                   SerializadorEventosPort serializador) {
        this.repositorio = repositorio;
        this.outbox = outbox;
        this.serializador = serializador;
    }

    @Override
    public void asignarTarifaAMedidor(String medidorSerial) {
        if (medidorSerial == null || medidorSerial.isBlank()) return;
        Tarifa tarifa = tarifaVigenteOPorDefecto();
        Vigencia vigencia = Vigencia.desde(Instant.now());

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("tarifaId", tarifa.id().valor());
        payload.put("nombreTarifa", tarifa.nombre());
        payload.put("medidorSerial", medidorSerial);
        payload.put("vigenciaDesde", vigencia.desde().toString());
        outbox.agregar(MensajeOutbox.pendiente("TarifaAsignada", medidorSerial, serializador.aJson(payload)));
    }

    @Override
    public void registrarConsumoTarificado(String medidorSerial, double consumoNetoKwh, int hora) {
        if (medidorSerial == null) return;
        Tarifa tarifa = tarifaVigenteOPorDefecto();
        double precio = tarifa.precioEn(normalizarHora(hora)).porKwh().doubleValue();
        double cargo = consumoNetoKwh * precio;

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("tarifaId", tarifa.id().valor());
        payload.put("medidorSerial", medidorSerial);
        payload.put("consumoNetoKwh", String.valueOf(consumoNetoKwh));
        payload.put("precioKwh", String.valueOf(precio));
        payload.put("cargo", String.valueOf(cargo));
        payload.put("tarificadoEn", Instant.now().toString());
        outbox.agregar(MensajeOutbox.pendiente("ConsumoTarificado", medidorSerial, serializador.aJson(payload)));
    }

    /** Toma la primera tarifa registrada; si no hay ninguna, crea una por defecto (24h plano). */
    private Tarifa tarifaVigenteOPorDefecto() {
        List<Tarifa> tarifas = repositorio.listar();
        if (!tarifas.isEmpty()) return tarifas.get(0);
        Tarifa porDefecto = Tarifa.crear("Tarifa plana por defecto",
                List.of(FranjaHoraria.de(0, 24, Precio.porKwh(java.math.BigDecimal.valueOf(0.5)))));
        return repositorio.guardar(porDefecto);
    }

    private int normalizarHora(int hora) {
        if (hora < 0) return 0;
        if (hora > 23) return 23;
        return hora;
    }
}
