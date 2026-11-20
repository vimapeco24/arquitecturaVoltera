package com.voltera.ia.infrastructure.eventos;

import com.voltera.ia.infrastructure.knowledge.Bm25ConocimientoDer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Consumidores Kafka contra un broker embebido real (no solo el puente HTTP). */
@SpringBootTest(properties = {
        "voltera.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "eureka.client.enabled=false"})
@EmbeddedKafka(partitions = 1, topics = {EventosEntrantesService.TOPIC_ALERTAS, EventosEntrantesService.TOPIC_CONOCIMIENTO})
class EventosKafkaConsumerIT {

    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired Bm25ConocimientoDer rag;

    private static void esperar(java.util.function.BooleanSupplier c) throws InterruptedException {
        Instant limite = Instant.now().plus(Duration.ofSeconds(30));
        while (!c.getAsBoolean() && Instant.now().isBefore(limite)) Thread.sleep(200);
    }

    @Test
    void eventoDeConocimientoPorKafkaActualizaElIndiceDelRag() throws Exception {
        String doc = "id: kafka-it\\ntitulo: Doc por Kafka\\ncategoria: procedimientos\\nactivos: MEDIDOR\\n---\\n## Seccion\\nquetzalinfinito unico";
        kafka.send(EventosEntrantesService.TOPIC_CONOCIMIENTO, "k",
                "{\"tipo\":\"DocumentoConocimientoPublicado\",\"contenido\":\"" + doc + "\"}").get();
        esperar(() -> rag.documentos().stream().anyMatch(d -> d.id().equals("kafka-it")));
        assertTrue(rag.documentos().stream().anyMatch(d -> d.id().equals("kafka-it")), "el consumidor Kafka debe ingerir el documento");
        kafka.send(EventosEntrantesService.TOPIC_CONOCIMIENTO, "k",
                "{\"tipo\":\"DocumentoConocimientoRetirado\",\"documentoId\":\"kafka-it\"}").get();
        esperar(() -> rag.documentos().stream().noneMatch(d -> d.id().equals("kafka-it")));
        assertTrue(rag.documentos().stream().noneMatch(d -> d.id().equals("kafka-it")), "la supresión debe propagarse");
    }
}
