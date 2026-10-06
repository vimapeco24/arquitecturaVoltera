package com.voltera.notificaciones.infrastructure.persistence;

import com.voltera.notificaciones.domain.model.Preferencia;
import com.voltera.notificaciones.domain.port.out.PreferenciaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Preferencias por medidor en memoria. Si no hay preferencia registrada se usa la
 * de por defecto (EMAIL + suscrito a todas las alertas).
 */
@Repository
public class InMemoryPreferenciaRepository implements PreferenciaRepositoryPort {

    private final ConcurrentHashMap<String, Preferencia> datos = new ConcurrentHashMap<>();

    @Override
    public Preferencia preferenciaDe(String medidorId) {
        return datos.getOrDefault(medidorId, Preferencia.porDefecto());
    }

    @Override
    public void registrar(String medidorId, Preferencia preferencia) {
        datos.put(medidorId, preferencia);
    }
}
