package com.voltera.telemetriacore.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.telemetriacore.domain.model.ConsumoAgregadoVista;
import com.voltera.telemetriacore.domain.port.out.ConsumoAgregadoRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador Redis de la VISTA AGREGADA (lado QUERY, lamina 03: "vistas agregadas").
 * Activo con el perfil {@code redis}.
 *
 * <p>Modelo de claves: un unico <b>Hash</b> {@code tc:agregado} cuyos campos son el
 * {@code medidorSerial} y el valor el JSON de la {@link ConsumoAgregadoVista}. El
 * acumulado por medidor queda disponible en O(1) para la API de lectura.</p>
 */
@Repository
@Profile("redis")
public class RedisConsumoAgregadoRepository implements ConsumoAgregadoRepositoryPort {

    private static final String CLAVE = "tc:agregado";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisConsumoAgregadoRepository(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public ConsumoAgregadoVista guardar(ConsumoAgregadoVista vista) {
        try {
            redis.opsForHash().put(CLAVE, vista.medidorSerial(), mapper.writeValueAsString(vista));
            return vista;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar ConsumoAgregadoVista en Redis", e);
        }
    }

    @Override
    public Optional<ConsumoAgregadoVista> porMedidor(String medidorSerial) {
        Object v = redis.opsForHash().get(CLAVE, medidorSerial);
        return (v == null) ? Optional.empty() : Optional.of(deserializar((String) v));
    }

    @Override
    public List<ConsumoAgregadoVista> todas() {
        Map<Object, Object> todas = redis.opsForHash().entries(CLAVE);
        List<ConsumoAgregadoVista> salida = new ArrayList<>(todas.size());
        for (Object v : todas.values()) {
            salida.add(deserializar((String) v));
        }
        return salida;
    }

    private ConsumoAgregadoVista deserializar(String json) {
        try {
            return mapper.readValue(json, ConsumoAgregadoVista.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer ConsumoAgregadoVista de Redis", e);
        }
    }
}
