package com.voltera.habilitacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.habilitacion.domain.port.in.HabilitarMedidorUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code telemetria-lecturas-validadas} y reacciona solo a
 * {@code CanalIngestaCreado} (de Ingesta): confirma el canal del medidor para
 * avanzar el alta (lamina 02: Habilitacion consume CanalIngestaCreado).
 *
 * <p>En ese topic Ingesta publica LecturaValidada, LecturaSospechosaDetectada y
 * CanalIngestaCreado; discriminamos CanalIngestaCreado porque trae 'creadoEn' y
 * no trae 'validadaEn' ni 'motivo'.</p>
 */
@Component
@Lazy(false)
public class CanalIngestaConsumer {

    private static final Logger log = LoggerFactory.getLogger(CanalIngestaConsumer.class);

    private final HabilitarMedidorUseCase useCase;
    private final ObjectMapper objectMapper;

    public CanalIngestaConsumer(HabilitarMedidorUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telemetria-lecturas-validadas", groupId = "habilitacion-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            boolean esCanalCreado = n.hasNonNull("creadoEn")
                    && !n.hasNonNull("validadaEn")
                    && !n.hasNonNull("motivo");
            if (!esCanalCreado) return;
            String serial = n.hasNonNull("medidorSerial") ? n.get("medidorSerial").asText() : null;
            if (serial == null) return;
            useCase.confirmarCanalIngestaPorSerial(serial);
            log.info("CanalIngestaCreado confirmado por evento. serial={}", serial);
        } catch (Exception e) {
            log.warn("No se pudo procesar CanalIngestaCreado (serial no habilitado?). payload={} err={}",
                    mensaje, e.getMessage());
        }
    }
}
