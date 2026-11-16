package com.voltera.ia.infrastructure.persistence;

import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.out.PropuestaRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State &amp; Memory en memoria para el reflejo off-chain de las propuestas. La
 * fuente de verdad es la cadena; esto es el estado del flujo que consultan la
 * consola y la app. En producción sería un System of Record persistente.
 */
@Component
public class InMemoryPropuestaRepository implements PropuestaRepositoryPort {

    private final Map<UUID, PropuestaAccion> store = new ConcurrentHashMap<>();

    @Override
    public PropuestaAccion guardar(PropuestaAccion propuesta) {
        store.put(propuesta.propuestaId(), propuesta);
        return propuesta;
    }

    @Override
    public Optional<PropuestaAccion> porId(UUID propuestaId) {
        return Optional.ofNullable(store.get(propuestaId));
    }

    @Override
    public List<PropuestaAccion> listar() {
        return new ArrayList<>(store.values());
    }
}
