package com.voltera.telemetriacore.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.telemetriacore.domain.event.LecturaValidada;
import com.voltera.telemetriacore.domain.port.in.RegistrarConsumoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consume {@code telemetria-lecturas-validadas} (emitido por Ingesta). Solo procesa
 * los eventos LecturaValidada (ignora CanalIngestaCreado y LecturaSospechosaDetectada,
 * que no llevan consumo a la serie). Incorpora la lectura a la serie del medidor.
 */
@Component
@Lazy(false)
public class LecturaValidadaConsumer {

    private static final Logger log = LoggerFactory.getLogger(LecturaValidadaConsumer.class);

    private final RegistrarConsumoUseCase useCase;
    private final ObjectMapper objectMapper;

    public LecturaValidadaConsumer(RegistrarConsumoUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telemetria-lecturas-validadas", groupId = "telemetria-core-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            // Solo LecturaValidada trae 'validadaEn' + 'consumoKwh'; los demas eventos se ignoran.
            if (!n.hasNonNull("validadaEn") || !n.hasNonNull("consumoKwh")) {
                return;
            }
            LecturaValidada lectura = new LecturaValidada(
                    texto(n, "eventId"),
                    texto(n, "medidorSerial"),
                    n.get("consumoKwh").asDouble(),
                    parseInstant(texto(n, "capturadaEn")),
                    parseInstant(texto(n, "validadaEn"))
            );
            useCase.registrarLectura(lectura);
            log.info("LecturaValidada incorporada a la serie. serial={}, kwh={}",
                    lectura.medidorSerial(), lectura.consumoKwh());
        } catch (Exception e) {
            log.error("Error procesando LecturaValidada. payload={}", mensaje, e);
        }
    }

    private String texto(JsonNode n, String campo) {
        return n.hasNonNull(campo) ? n.get(campo).asText() : null;
    }

    private Instant parseInstant(String s) {
        try { return s != null ? Instant.parse(s) : Instant.now(); }
        catch (Exception e) { return Instant.now(); }
    }
}
