package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia del reflejo off-chain de las propuestas (State &amp; Memory). La
 * fuente de verdad es la cadena; aquí se guarda el estado del flujo entre agentes
 * y firmantes (0/3 → 3/3) para que la consola y la app lo consulten.
 */
public interface PropuestaRepositoryPort {

    PropuestaAccion guardar(PropuestaAccion propuesta);

    Optional<PropuestaAccion> porId(UUID propuestaId);

    List<PropuestaAccion> listar();
}
