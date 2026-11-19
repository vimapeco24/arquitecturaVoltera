package com.voltera.ia.infrastructure.notificaciones;

import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.out.NotificadorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Agente Notificador (4º agente). Envía notificaciones contextualizadas a los
 * firmantes humanos con un resumen en lenguaje adecuado a cada audiencia y escala
 * al MISMO firmante pendiente cuando una firma no llega a tiempo. Detrás vive MS
 * Notificaciones (E2). Canales: bandeja dentro de la app (siempre), log y, si se
 * define {@code IA_NOTIF_WEBHOOK_URL}, un webhook (Slack/Discord/Teams) best-effort.
 *
 * <p>Clave del documento: la escalación NUNCA transfiere la decisión a otra
 * persona; reenvía con urgencia elevada al mismo firmante.</p>
 */
@Component
public class NotificacionesAdapter implements NotificadorPort {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesAdapter.class);
    private final BandejaNotificaciones bandeja;
    private final String webhookUrl;
    private final WebClient http;

    public NotificacionesAdapter(BandejaNotificaciones bandeja,
                                 @Value("${ia.notificaciones.webhook-url:}") String webhookUrl) {
        this.bandeja = bandeja;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.http = WebClient.builder().build();
    }

    /** Entrega en la bandeja y, si hay webhook, lo envía sin bloquear el flujo. */
    private String entregar(String destinatario, String tipo, PropuestaAccion p, String msg, boolean urgente) {
        String canalWebhook = webhookUrl.isEmpty() ? "no configurado" : "enviado";
        var n = bandeja.agregar(destinatario, tipo, p.propuestaId().toString(), p.activoId(),
                p.estado().name(), p.firmasResumen(), msg, urgente, canalWebhook);
        if (!webhookUrl.isEmpty()) {
            http.post().uri(webhookUrl).bodyValue(Map.of("text", "[Voltera · " + destinatario + "] " + msg))
                    .retrieve().toBodilessEntity().timeout(Duration.ofSeconds(5))
                    .subscribe(r -> { }, e -> log.warn("[notificador] webhook falló: {}", e.getMessage()));
        }
        return "notif:" + n.id();
    }

    @Override
    public String notificarFirmantePendiente(PropuestaAccion p, Firmante firmante) {
        String msg = resumen(p, firmante, false);
        log.info("[notificador] firmante={} propuesta={} activo={} firmas={} -> {}",
                firmante, p.propuestaId(), p.activoId(), p.firmasResumen(), msg);
        return entregar(firmante.name(), "FIRMA_PENDIENTE", p, msg, false);
    }

    @Override
    public String escalar(PropuestaAccion p, Firmante firmante) {
        String msg = resumen(p, firmante, true);
        log.warn("[notificador·escalacion] MISMO firmante={} propuesta={} activo={} (urgencia elevada) -> {}",
                firmante, p.propuestaId(), p.activoId(), msg);
        return entregar(firmante.name(), "ESCALACION", p, msg, true);
    }

    @Override
    public String notificarDesenlace(PropuestaAccion p) {
        log.info("[notificador] desenlace propuesta={} activo={} estado={} firmas={}",
                p.propuestaId(), p.activoId(), p.estado(), p.firmasResumen());
        String msg = "La propuesta sobre " + p.activoId() + " terminó en " + p.estado()
                + " (" + p.firmasResumen() + " firmas)" + (p.resultadoEjecucion() != null ? ". " + p.resultadoEjecucion() : ".");
        return entregar("TODOS", "DESENLACE", p, msg, false);
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
