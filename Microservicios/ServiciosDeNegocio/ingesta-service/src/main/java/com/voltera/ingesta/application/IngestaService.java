package com.voltera.ingesta.application;

import com.voltera.ingesta.domain.event.LecturaCrudaRecibida;
import com.voltera.ingesta.domain.model.MensajeOutbox;
import com.voltera.ingesta.domain.model.ResultadoValidacion;
import com.voltera.ingesta.domain.model.SesionDeIngesta;
import com.voltera.ingesta.domain.port.in.IngestarLecturaUseCase;
import com.voltera.ingesta.domain.port.out.OutboxPort;
import com.voltera.ingesta.domain.port.out.SerializadorEventosPort;
import com.voltera.ingesta.domain.port.out.SesionRepositoryPort;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MS Ingesta de telemetria (BC Telemetria · Ingesta, lamina 02). Valida la
 * {@code LecturaCrudaRecibida} contra la regla y la ventana de duplicados, y emite
 * via OUTBOX: {@code LecturaValidada} (ok), {@code LecturaSospechosaDetectada}
 * (fuera de rango) o {@code CanalIngestaCreado} (al abrir canal del medidor).
 */
public class IngestaService implements IngestarLecturaUseCase {

    private final SesionRepositoryPort sesiones;
    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;

    public IngestaService(SesionRepositoryPort sesiones, OutboxPort outbox,
                          SerializadorEventosPort serializador) {
        this.sesiones = sesiones;
        this.outbox = outbox;
        this.serializador = serializador;
    }

    @Override
    public ResultadoValidacion procesar(LecturaCrudaRecibida lectura) {
        SesionDeIngesta sesion = sesiones.obtener();
        ResultadoValidacion resultado = sesion.procesar(
                lectura.medidorSerial(), lectura.consumoKwh(), lectura.capturadaEn());

        switch (resultado) {
            case VALIDADA -> encolar("LecturaValidada", lectura.medidorSerial(), Map.of(
                    "eventId", "EVT-" + UUID.randomUUID(),
                    "medidorSerial", lectura.medidorSerial(),
                    "consumoKwh", String.valueOf(lectura.consumoKwh()),
                    "capturadaEn", String.valueOf(lectura.capturadaEn()),
                    "validadaEn", Instant.now().toString()
            ));
            case SOSPECHOSA -> encolar("LecturaSospechosaDetectada", lectura.medidorSerial(), Map.of(
                    "eventId", "EVT-" + UUID.randomUUID(),
                    "medidorSerial", lectura.medidorSerial(),
                    "consumoKwh", String.valueOf(lectura.consumoKwh()),
                    "motivo", "Fuera del rango [" + sesion.regla().minKwh() + ", " + sesion.regla().maxKwh() + "] kWh"
            ));
            case DUPLICADA, NO_HABILITADO -> { /* se descarta sin emitir */ }
        }
        return resultado;
    }

    @Override
    public void abrirCanal(String medidorSerial) {
        SesionDeIngesta sesion = sesiones.obtener();
        if (sesion.abrirCanal(medidorSerial)) {
            encolar("CanalIngestaCreado", medidorSerial, Map.of(
                    "medidorSerial", medidorSerial,
                    "creadoEn", Instant.now().toString()
            ));
        }
    }

    @Override
    public void cerrarCanal(String medidorSerial) {
        sesiones.obtener().cerrarCanal(medidorSerial);
    }

    @Override
    public SesionDeIngesta estado() {
        return sesiones.obtener();
    }

    private void encolar(String tipoEvento, String clave, Map<String, String> payload) {
        outbox.agregar(MensajeOutbox.pendiente(tipoEvento, clave, serializador.aJson(payload)));
    }
}
