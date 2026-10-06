package com.voltera.habilitacion.application;

import com.voltera.habilitacion.domain.event.OrdenInstalacionCerrada;
import com.voltera.habilitacion.domain.exception.MedidorNoEncontradoException;
import com.voltera.habilitacion.domain.model.EstadoHabilitacion;
import com.voltera.habilitacion.domain.model.DatosComerciales;
import com.voltera.habilitacion.domain.model.IdentidadDispositivo;
import com.voltera.habilitacion.domain.model.Medidor;
import com.voltera.habilitacion.domain.model.MensajeOutbox;
import com.voltera.habilitacion.domain.model.PuntoDeMedicion;
import com.voltera.habilitacion.domain.port.in.HabilitarMedidorUseCase;
import com.voltera.habilitacion.domain.port.out.MedidorRepositoryPort;
import com.voltera.habilitacion.domain.port.out.OutboxPort;
import com.voltera.habilitacion.domain.port.out.SerializadorEventosPort;

import java.time.Instant;
import java.util.Map;

/**
 * Mediador del alta de medidores (BC Habilitacion, lamina 02). Reacciona a
 * {@code OrdenInstalacionCerrada}, emite {@code MedidorHabilitado} (estado completo)
 * y orquesta: espera {@code CanalIngestaCreado} + {@code TarifaAsignada}. Con ambas
 * emite {@code MedidorActivado}; si vence el plazo (15 min), {@code HabilitacionFallida}
 * + {@code MedidorSuspendido}. Todos los eventos salen por el OUTBOX transaccional.
 */
public class HabilitacionService implements HabilitarMedidorUseCase {

    private final MedidorRepositoryPort repositorio;
    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;

    public HabilitacionService(MedidorRepositoryPort repositorio, OutboxPort outbox,
                               SerializadorEventosPort serializador) {
        this.repositorio = repositorio;
        this.outbox = outbox;
        this.serializador = serializador;
    }

    @Override
    public Medidor habilitarDesdeOrden(OrdenInstalacionCerrada orden) {
        Medidor medidor = Medidor.habilitar(
                orden.medidorId(),
                new IdentidadDispositivo(orden.serial(), orden.fabricante()),
                new PuntoDeMedicion(orden.codigoPunto(), orden.direccion()),
                new DatosComerciales(
                        orden.clienteId(),
                        orden.proveedorAmi(),
                        orden.protocolo(),
                        orden.plan(),
                        orden.canales()),
                orden.ordenInstalacionId());
        repositorio.guardar(medidor);

        // MedidorHabilitado lleva el ESTADO COMPLETO (event-carried state transfer, lamina 03):
        // cliente, proveedor, protocolo, punto de medicion, plan y canales.
        DatosComerciales dc = medidor.datosComerciales();
        encolar("MedidorHabilitado", medidor.medidorId(), Map.of(
                "medidorId", medidor.medidorId(),
                "serial", medidor.identidad().serial(),
                "codigoPunto", medidor.punto().codigoPunto(),
                "clienteId", dc.clienteId(),
                "proveedorAmi", dc.proveedorAmi(),
                "protocolo", dc.protocolo(),
                "plan", dc.plan(),
                "canales", dc.canalesComoTexto(),
                // 'estado' se conserva: es el discriminador que usan los consumidores
                // (p.ej. notificaciones) para distinguir MedidorHabilitado de los demas
                // eventos del topic medidores-habilitacion.
                "estado", medidor.estado().name()
        ));
        return medidor;
    }

    @Override
    public Medidor registrarCanalIngestaCreado(String medidorId) {
        Medidor medidor = obtener(medidorId);
        medidor.registrarCanalIngestaCreado();
        repositorio.guardar(medidor);
        emitirActivadoSiAplica(medidor);
        return medidor;
    }

    @Override
    public Medidor registrarTarifaAsignada(String medidorId) {
        Medidor medidor = obtener(medidorId);
        medidor.registrarTarifaAsignada();
        repositorio.guardar(medidor);
        emitirActivadoSiAplica(medidor);
        return medidor;
    }

    @Override
    public Medidor confirmarCanalIngestaPorSerial(String serial) {
        Medidor medidor = repositorio.buscarPorSerial(serial)
                .orElseThrow(() -> new MedidorNoEncontradoException("serial=" + serial));
        return registrarCanalIngestaCreado(medidor.medidorId());
    }

    @Override
    public Medidor confirmarTarifaPorSerialOId(String serialOId) {
        Medidor medidor = repositorio.buscarPorId(serialOId)
                .or(() -> repositorio.buscarPorSerial(serialOId))
                .orElseThrow(() -> new MedidorNoEncontradoException(serialOId));
        return registrarTarifaAsignada(medidor.medidorId());
    }

    @Override
    public int procesarTimeouts() {
        Instant ahora = Instant.now();
        int suspendidos = 0;
        for (Medidor medidor : repositorio.pendientes()) {
            if (medidor.suspenderPorTimeout(ahora)) {
                repositorio.guardar(medidor);
                emitirSuspension(medidor);
                suspendidos++;
            }
        }
        return suspendidos;
    }

    @Override
    public Medidor forzarTimeout(String medidorId) {
        Medidor medidor = obtener(medidorId);
        if (medidor.forzarSuspension()) {
            repositorio.guardar(medidor);
            emitirSuspension(medidor);
        }
        return medidor;
    }

    private void emitirActivadoSiAplica(Medidor medidor) {
        if (medidor.estado() == EstadoHabilitacion.ACTIVADO) {
            encolar("MedidorActivado", medidor.medidorId(), Map.of(
                    "medidorId", medidor.medidorId(),
                    "activadoEn", String.valueOf(medidor.finalizadoEn())
            ));
        }
    }

    private void emitirSuspension(Medidor medidor) {
        encolar("HabilitacionFallida", medidor.medidorId(), Map.of(
                "medidorId", medidor.medidorId(),
                "motivo", String.valueOf(medidor.motivoFallo())
        ));
        encolar("MedidorSuspendido", medidor.medidorId(), Map.of(
                "medidorId", medidor.medidorId(),
                "motivo", String.valueOf(medidor.motivoFallo())
        ));
    }

    private void encolar(String tipoEvento, String clave, Map<String, String> payload) {
        outbox.agregar(MensajeOutbox.pendiente(tipoEvento, clave, serializador.aJson(payload)));
    }

    private Medidor obtener(String medidorId) {
        return repositorio.buscarPorId(medidorId)
                .orElseThrow(() -> new MedidorNoEncontradoException(medidorId));
    }
}
