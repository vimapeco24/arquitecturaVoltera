package com.voltera.telemetriacore.infrastructure.persistence;

import com.voltera.telemetriacore.domain.model.ConsumoAgregadoVista;
import com.voltera.telemetriacore.domain.port.out.ConsumoAgregadoRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de la vista agregada (lado QUERY). In-memory por defecto; se desactiva
 * con el perfil 'redis' (lo reemplaza {@code RedisConsumoAgregadoRepository}).
 * Representa la "Vista de lectura · Redis (en vivo) + vistas agregadas" de la lámina 03.
 */
@Repository
@Profile("!redis")
public class InMemoryConsumoAgregadoRepository implements ConsumoAgregadoRepositoryPort {

    private final ConcurrentHashMap<String, ConsumoAgregadoVista> porMedidor = new ConcurrentHashMap<>();

    @Override
    public ConsumoAgregadoVista guardar(ConsumoAgregadoVista vista) {
        porMedidor.put(vista.medidorSerial(), vista);
        return vista;
    }

    @Override
    public Optional<ConsumoAgregadoVista> porMedidor(String medidorSerial) {
        return Optional.ofNullable(porMedidor.get(medidorSerial));
    }

    @Override
    public List<ConsumoAgregadoVista> todas() {
        return List.copyOf(porMedidor.values());
    }
}
