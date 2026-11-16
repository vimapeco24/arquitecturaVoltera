package com.voltera.ia.infrastructure.notificaciones;

import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.out.NotificadorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Agente Notificador (4º agente). Envía notificaciones contextualizadas a los
 * firmantes humanos con un resumen en lenguaje adecuado a cada audiencia y escala
 * al MISMO firmante pendiente cuando una firma no llega a tiempo. Detrás vive MS
 * Notificaciones (E2); aquí se registra por log (best-effort) y en producción se
 * despacha push/correo/consola con enlace directo a la pantalla de firma.
 *
 * <p>Clave del documento: la escalación NUNCA transfiere la decisión a otra
 * persona; reenvía con urgencia elevada al mismo firmante.</p>
 */
@Component
public class NotificacionesAdapter implements NotificadorPort {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesAdapter.class);

    @Override
    public String notificarFirmantePendiente(PropuestaAccion p, Firmante firmante) {
        String msg = resumen(p, firmante, false);
        log.info("[notificador] firmante={} propuesta={} activo={} firmas={} -> {}",
                firmante, p.propuestaId(), p.activoId(), p.firmasResumen(), msg);
        return "notif:" + firmante + ":" + p.propuestaId();
    }

    @Override
    public String escalar(PropuestaAccion p, Firmante firmante) {
        String msg = resumen(p, firmante, true);
        log.warn("[notificador·escalacion] MISMO firmante={} propuesta={} activo={} (urgencia elevada) -> {}",
                firmante, p.propuestaId(), p.activoId(), msg);
        return "escal:" + firmante + ":" + p.propuestaId();
    }

    @Override
    public String notificarDesenlace(PropuestaAccion p) {
        log.info("[notificador] desenlace propuesta={} activo={} estado={} firmas={}",
                p.propuestaId(), p.activoId(), p.estado(), p.firmasResumen());
        return "notif:desenlace:" + p.propuestaId();
    }

    private String resumen(PropuestaAccion p, Firmante firmante, boolean urgente) {
        String base = "Acción propuesta " + p.tipoAccion() + " sobre " + p.activoId()
                + " (severidad " + p.severidad() + "). Requiere su firma " + etapa(firmante)
                + ". Enlace directo a la pantalla de revisión y firma.";
        return urgente ? ("URGENTE · " + base) : base;
    }

    private String etapa(Firmante firmante) {
        return switch (firmante) {
            case OPERADOR -> "técnica (2/3)";
            case PROSUMIDOR -> "de consentimiento (3/3)";
            case AGENTE_IA -> "del sistema (1/3)";
        };
    }
}
