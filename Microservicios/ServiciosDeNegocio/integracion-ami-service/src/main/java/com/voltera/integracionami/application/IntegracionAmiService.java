package com.voltera.integracionami.application;

import com.voltera.integracionami.domain.model.ConexionHeadEnd;
import com.voltera.integracionami.domain.model.LecturaCruda;
import com.voltera.integracionami.domain.model.MensajeOutbox;
import com.voltera.integracionami.domain.port.in.RecibirLecturasUseCase;
import com.voltera.integracionami.domain.port.out.ConexionRepositoryPort;
import com.voltera.integracionami.domain.port.out.OutboxPort;
import com.voltera.integracionami.domain.port.out.SerializadorEventosPort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador ACL del BC Integracion AMI (lamina 02). Traduce lecturas crudas del
 * head-end a formato canonico (kWh) y emite {@code LecturaCrudaRecibida} por el
 * outbox, solo para medidores atendidos (habilitados). Puede emitir
 * {@code ProveedorDegradado} ante fallas del proveedor.
 */
public class IntegracionAmiService implements RecibirLecturasUseCase {

    private final ConexionRepositoryPort conexiones;
    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;

    public IntegracionAmiService(ConexionRepositoryPort conexiones, OutboxPort outbox,
                                 SerializadorEventosPort serializador) {
        this.conexiones = conexiones;
        this.outbox = outbox;
        this.serializador = serializador;
    }

    @Override
    public int recibirLote(String proveedorAmi, List<LecturaCruda> lote) {
        ConexionHeadEnd conexion = conexiones.obtener();
        int emitidas = 0;
        for (LecturaCruda cruda : lote) {
            // ACL: solo se traducen lecturas de medidores que atendemos (habilitados).
            if (!conexion.atiende(cruda.medidorSerial())) {
                continue;
            }
            double consumoKwh = aKwh(cruda.valor(), cruda.unidad());
            encolar("LecturaCrudaRecibida", cruda.medidorSerial(), Map.of(
                    "eventId", "EVT-" + UUID.randomUUID(),
                    "medidorSerial", cruda.medidorSerial(),
                    "consumoKwh", String.valueOf(consumoKwh),
                    "proveedorAmi", proveedorAmi,
                    "capturadaEn", cruda.capturadaEn().toString(),
                    "recibidaEn", Instant.now().toString()
            ));
            emitidas++;
        }
        return emitidas;
    }

    /** Traduccion de unidad del proveedor a formato canonico (kWh). */
    private double aKwh(double valor, String unidad) {
        if (unidad == null) return valor;
        return switch (unidad.trim().toUpperCase()) {
            case "WH" -> valor / 1000.0;
            case "MWH" -> valor * 1000.0;
            default -> valor; // "KWH" o desconocido: se asume canonico
        };
    }

    @Override
    public void habilitarMedidor(String medidorSerial) {
        ConexionHeadEnd conexion = conexiones.obtener();
        conexion.habilitarMedidor(medidorSerial);
        conexiones.guardar(conexion);
    }

    @Override
    public void suspenderMedidor(String medidorSerial) {
        ConexionHeadEnd conexion = conexiones.obtener();
        conexion.suspenderMedidor(medidorSerial);
        conexiones.guardar(conexion);
    }

    @Override
    public ConexionHeadEnd reportarDegradacion(String motivo) {
        ConexionHeadEnd conexion = conexiones.obtener();
        conexion.marcarDegradado();
        conexiones.guardar(conexion);
        encolar("ProveedorDegradado", conexion.proveedorAmi(), Map.of(
                "proveedorAmi", conexion.proveedorAmi(),
                "protocolo", conexion.protocolo().name(),
                "motivo", String.valueOf(motivo),
                "detectadoEn", Instant.now().toString()
        ));
        return conexion;
    }

    @Override
    public ConexionHeadEnd estado() {
        return conexiones.obtener();
    }

    private void encolar(String tipoEvento, String clave, Map<String, String> payload) {
        outbox.agregar(MensajeOutbox.pendiente(tipoEvento, clave, serializador.aJson(payload)));
    }
}
