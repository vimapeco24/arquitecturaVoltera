package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.gobernanza.EventoGobernanza;

/**
 * Publica los eventos del ciclo de gobernanza al tópico {@code der.gobernanza}.
 * Best-effort: en la nube cae al publicador de log cuando el broker on-premise
 * no es alcanzable.
 */
public interface GobernanzaEventPublisherPort {

    boolean publicar(EventoGobernanza evento);
}
