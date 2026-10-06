package com.voltera.notificaciones.infrastructure.persistence;

import com.voltera.notificaciones.domain.model.Notificacion;
import com.voltera.notificaciones.domain.port.out.NotificacionRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryNotificacionRepository implements NotificacionRepositoryPort {

    private final ConcurrentHashMap<String, Notificacion> datos = new ConcurrentHashMap<>();

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        datos.put(notificacion.notificacionId(), notificacion);
        return notificacion;
    }

    @Override
    public List<Notificacion> todas() {
        return datos.values().stream()
                .sorted(Comparator.comparing(Notificacion::generadaEn))
                .toList();
    }
}
