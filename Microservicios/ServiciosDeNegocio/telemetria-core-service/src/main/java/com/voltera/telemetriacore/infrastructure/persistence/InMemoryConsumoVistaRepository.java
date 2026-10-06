package com.voltera.telemetriacore.infrastructure.persistence;

import com.voltera.telemetriacore.domain.model.ConsumoVista;
import com.voltera.telemetriacore.domain.port.out.ConsumoVistaRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Adaptador por defecto (in-memory) de la vista por intervalo. Se desactiva con el perfil 'redis'. */
@Repository
@Profile("!redis")
public class InMemoryConsumoVistaRepository implements ConsumoVistaRepositoryPort {

    private final ConcurrentHashMap<String, ConsumoVista> porId = new ConcurrentHashMap<>();

    @Override
    public ConsumoVista guardar(ConsumoVista vista) {
        porId.put(vista.id(), vista);
        return vista;
    }

    @Override
    public List<ConsumoVista> porMedidor(String medidorSerial) {
        return porId.values().stream()
                .filter(v -> v.medidorSerial().equals(medidorSerial))
                .toList();
    }

    @Override
    public List<ConsumoVista> todas() {
        return List.copyOf(porId.values());
    }
}
