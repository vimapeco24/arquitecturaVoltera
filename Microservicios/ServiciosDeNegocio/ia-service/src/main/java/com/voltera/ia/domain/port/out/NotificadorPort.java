package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;

/**
 * Cuarto agente del flujo: Agente Notificador. Envía notificaciones
 * contextualizadas a los firmantes humanos (operador y prosumidor) con el
 * resumen del diagnóstico y la acción propuesta, en lenguaje adecuado a cada
 * audiencia, y escala al MISMO firmante pendiente cuando una firma no llega a
 * tiempo. La escalación nunca transfiere la decisión a otra persona.
 *
 * <p>Detrás vive MS Notificaciones (E2, modificado: plantillas de firma con
 * enlace directo). Best-effort: devuelve un identificador de envío para la traza.</p>
 */
public interface NotificadorPort {

    /** Notifica a un firmante que tiene una propuesta pendiente de su firma. */
    String notificarFirmantePendiente(PropuestaAccion propuesta, Firmante firmante);

    /** Escala con urgencia elevada al MISMO firmante pendiente (timeout de firma). */
    String escalar(PropuestaAccion propuesta, Firmante firmante);

    /** Notifica el desenlace (aprobada/ejecutada/rechazada/vencida) a los involucrados. */
    String notificarDesenlace(PropuestaAccion propuesta);
}
