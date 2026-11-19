package com.voltera.ia.infrastructure.audit;

import com.voltera.ia.domain.port.out.GuardrailsPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Caja 8 · Audit &amp; Retention. Registro consultable de lo que hace el módulo de IA:
 * diagnósticos, propuestas, firmas, cambios de versión de prompt, bajas de agentes,
 * correcciones humanas e ingesta de conocimiento. Política declarada: qué se registra,
 * con qué redacción (PII enmascarada, sin texto libre del usuario) y por cuánto tiempo.
 */
@Component
public class AuditoriaIa {

    /** Un registro de auditoría. {@code detalle} ya viene redactado. */
    public record Registro(String id, Instant instante, String tipo, String actor, String recurso,
                           String detalle, String version) {}

    private final List<Registro> registros = Collections.synchronizedList(new ArrayList<>());
    private final GuardrailsPort guardrails;
    private final int retencionDias;
    private final int maximo;

    public AuditoriaIa(GuardrailsPort guardrails,
                       @Value("${ia.auditoria.retencion-dias:365}") int retencionDias,
                       @Value("${ia.auditoria.maximo:5000}") int maximo) {
        this.guardrails = guardrails;
        this.retencionDias = retencionDias;
        this.maximo = maximo;
    }

    public void registrar(String tipo, String actor, String recurso, String detalle, String version) {
        String redactado = guardrails.enmascararPII(detalle == null ? "" : detalle);
        if (redactado.length() > 400) redactado = redactado.substring(0, 400) + "…";
        synchronized (registros) {
            registros.add(new Registro(UUID.randomUUID().toString(), Instant.now(), tipo,
                    actor == null || actor.isBlank() ? "sistema" : guardrails.enmascararPII(actor),
                    recurso, redactado, version));
            purgar();
        }
    }

    /** Elimina lo que supera el plazo de retención o el tope de registros. */
    private void purgar() {
        Instant limite = Instant.now().minus(Duration.ofDays(retencionDias));
        registros.removeIf(r -> r.instante().isBefore(limite));
        while (registros.size() > maximo) registros.remove(0);
    }

    public List<Registro> consultar(String tipo, String recurso, int limite) {
        synchronized (registros) {
            purgar();
            return registros.stream()
                    .filter(r -> tipo == null || tipo.isBlank() || r.tipo().equalsIgnoreCase(tipo))
                    .filter(r -> recurso == null || recurso.isBlank() || recurso.equals(r.recurso()))
                    .sorted(Comparator.comparing(Registro::instante).reversed())
                    .limit(Math.max(1, Math.min(limite, 500)))
                    .toList();
        }
    }

    public Map<String, Object> politica() {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        synchronized (registros) {
            for (Registro r : registros) porTipo.merge(r.tipo(), 1L, Long::sum);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("retencionDias", retencionDias);
        m.put("maximoRegistros", maximo);
        m.put("redaccion", "PII enmascarada (correos, documentos y números largos); el texto libre del usuario no se guarda, solo identificadores, hashes y versiones.");
        m.put("seRegistra", List.of("DIAGNOSTICO", "PROPUESTA", "FIRMA", "RECHAZO", "EJECUCION", "PROMOCION_PROMPT",
                "ROLLBACK_PROMPT", "AGENTE_ESTADO", "RETROALIMENTACION", "CONOCIMIENTO", "GUARDRAIL"));
        m.put("total", registros.size());
        m.put("porTipo", porTipo);
        return m;
    }
}
