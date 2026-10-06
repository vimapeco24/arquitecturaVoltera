package com.voltera.telemetriacore.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.telemetriacore.domain.model.ConsumoVista;
import com.voltera.telemetriacore.domain.port.out.ConsumoVistaRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador Redis de la VISTA POR INTERVALO (lado QUERY, lamina 03). Activo con el
 * perfil {@code redis}.
 *
 * <p>Modelo de claves: un <b>Hash</b> por medidor {@code tc:vista:{serial}} cuyos
 * campos son el id del intervalo ({@code serial@epoch}) y el valor el JSON de la
 * {@link ConsumoVista}. Un indice {@code tc:vista:index} (Set) lista los medidores
 * con vista para poder recorrer {@code todas()} sin usar KEYS.</p>
 */
@Repository
@Profile("redis")
public class RedisConsumoVistaRepository implements ConsumoVistaRepositoryPort {

    private static final String PREFIJO = "tc:vista:";
    private static final String INDICE = "tc:vista:index";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisConsumoVistaRepository(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public ConsumoVista guardar(ConsumoVista vista) {
        try {
            redis.opsForHash().put(PREFIJO + vista.medidorSerial(), vista.id(), mapper.writeValueAsString(vista));
            redis.opsForSet().add(INDICE, vista.medidorSerial());
            return vista;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar ConsumoVista en Redis", e);
        }
    }

    @Override
    public List<ConsumoVista> porMedidor(String medidorSerial) {
        List<Object> valores = redis.opsForHash().values(PREFIJO + medidorSerial);
        List<ConsumoVista> salida = new ArrayList<>(valores.size());
        for (Object v : valores) {
            salida.add(deserializar((String) v));
        }
        return salida;
    }

    @Override
    public List<ConsumoVista> todas() {
        List<ConsumoVista> salida = new ArrayList<>();
        var medidores = redis.opsForSet().members(INDICE);
        if (medidores != null) {
            for (String serial : medidores) {
                salida.addAll(porMedidor(serial));
            }
        }
        return salida;
    }

    private ConsumoVista deserializar(String json) {
        try {
            return mapper.readValue(json, ConsumoVista.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer ConsumoVista de Redis", e);
        }
    }
}
