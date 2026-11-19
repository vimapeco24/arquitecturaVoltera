package com.voltera.ia.infrastructure.notificaciones;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bandeja de notificaciones dentro de la app: cada aviso del Agente Notificador queda
 * aquí para su destinatario (operador, prosumidor o todos) hasta que lo marca como leído.
 */
@Component
public class BandejaNotificaciones {

    /** Aviso entregado a un destinatario. */
    public record Notificacion(String id, Instant instante, String destinatario, String tipo,
                               String propuestaId, String activoId, String estado, String firmas,
                               String mensaje, boolean urgente, boolean leida, String webhook) {
        Notificacion leer() {
            return new Notificacion(id, instante, destinatario, tipo, propuestaId, activoId, estado, firmas,
                    mensaje, urgente, true, webhook);
        }
    }

    private static final int MAX = 500;
    private final List<Notificacion> items = Collections.synchronizedList(new ArrayList<>());

    public Notificacion agregar(String destinatario, String tipo, String propuestaId, String activoId,
                                String estado, String firmas, String mensaje, boolean urgente, String webhook) {
        Notificacion n = new Notificacion(UUID.randomUUID().toString(), Instant.now(), destinatario, tipo,
                propuestaId, activoId, estado, firmas, mensaje, urgente, false, webhook);
        synchronized (items) {
            items.add(n);
            if (items.size() > MAX) items.remove(0);
        }
        return n;
    }

    public List<Notificacion> listar(String destinatario) {
        synchronized (items) {
            return items.stream()
                    .filter(n -> destinatario == null || destinatario.isBlank()
                            || n.destinatario().equalsIgnoreCase(destinatario) || n.destinatario().equals("TODOS"))
                    .sorted(Comparator.comparing(Notificacion::instante).reversed())
                    .toList();
        }
    }

    public Optional<Notificacion> marcarLeida(String id) {
        synchronized (items) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id().equals(id)) {
                    Notificacion l = items.get(i).leer();
                    items.set(i, l);
                    return Optional.of(l);
                }
            }
        }
        return Optional.empty();
    }
}
