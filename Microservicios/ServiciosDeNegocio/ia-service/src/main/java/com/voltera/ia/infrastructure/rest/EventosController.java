package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.infrastructure.eventos.EventosEntrantesService;
import com.voltera.ia.infrastructure.rest.dto.DiagnosticoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Puente HTTP de eventos para la nube, donde el broker Redpanda (LAN privada) no es alcanzable.
 * Entrega el MISMO evento que consume Kafka al mismo servicio de entrada; permite validar el
 * disparo por evento y la ingesta del RAG sin broker.
 */
@RestController
@RequestMapping("/api/v1/ia/eventos")
public class EventosController {

    private final EventosEntrantesService servicio;

    public EventosController(EventosEntrantesService servicio) {
        this.servicio = servicio;
    }

    /** Equivale a un MedidorSinReporte en {@code topic.notificaciones}. 202 sin cuerpo si es duplicado o no aplica. */
    @PostMapping("/medidor-sin-reporte")
    public ResponseEntity<?> medidorSinReporte(@RequestBody String evento) {
        return servicio.procesarAlerta(evento)
                .<ResponseEntity<?>>map(d -> ResponseEntity.ok(DiagnosticoResponse.desde(d)))
                .orElseGet(() -> ResponseEntity.accepted().body(Map.of("ignorado", true)));
    }

    /** Equivale a DocumentoConocimientoPublicado/Retirado en {@code topic.ia.conocimiento}. */
    @PostMapping("/conocimiento")
    public ResponseEntity<?> conocimiento(@RequestBody String evento) {
        return servicio.procesarConocimiento(evento)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("error", "evento no reconocido")));
    }
}
