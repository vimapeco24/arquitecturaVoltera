package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.infrastructure.notificaciones.BandejaNotificaciones;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Bandeja del Agente Notificador: avisos por destinatario (OPERADOR, PROSUMIDOR, TODOS). */
@RestController
@RequestMapping("/api/v1/ia/notificaciones")
public class NotificacionesController {

    private final BandejaNotificaciones bandeja;

    public NotificacionesController(BandejaNotificaciones bandeja) {
        this.bandeja = bandeja;
    }

    @GetMapping
    public ResponseEntity<List<BandejaNotificaciones.Notificacion>> listar(
            @RequestParam(value = "destinatario", required = false) String destinatario,
            @RequestParam(value = "propuestaId", required = false) String propuestaId) {
        return ResponseEntity.ok(bandeja.listar(destinatario).stream()
                .filter(n -> propuestaId == null || propuestaId.isBlank() || n.propuestaId().equals(propuestaId))
                .toList());
    }

    @PostMapping("/{id}/leida")
    public ResponseEntity<BandejaNotificaciones.Notificacion> leida(@PathVariable String id) {
        return bandeja.marcarLeida(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
