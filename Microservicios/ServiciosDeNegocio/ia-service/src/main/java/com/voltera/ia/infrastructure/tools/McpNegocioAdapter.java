package com.voltera.ia.infrastructure.tools;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Caja 3 · MCP Servers (servicios de negocio). Expone como herramientas de SOLO LECTURA
 * los microservicios que el taller declara para el caso: Tarifas, Facturación, Pagos y
 * Certificados de energía renovable. El agente no tiene herramienta de escritura sobre
 * ninguno (mínimo privilegio). Si el servicio no es alcanzable (p. ej. LAN privada desde
 * la nube) se degrada con elegancia y la evidencia queda marcada "no verificable".
 */
@Component
public class McpNegocioAdapter {

    private static final Logger log = LoggerFactory.getLogger(McpNegocioAdapter.class);

    private final boolean enabled;
    private final Duration timeout;
    private final WebClient tarifas;
    private final WebClient facturacion;
    private final WebClient pagos;
    private final WebClient certificados;

    public McpNegocioAdapter(
            @Value("${ia.tools.enabled:true}") boolean enabled,
            @Value("${ia.tools.timeout-ms:3000}") long timeoutMs,
            @Value("${ia.tools.tarifas-url:http://tarifas-service:8084}") String tarifasUrl,
            @Value("${ia.tools.facturacion-url:http://facturacion-service:8088}") String facturacionUrl,
            @Value("${ia.tools.pagos-url:http://pagos-service:8085}") String pagosUrl,
            @Value("${ia.tools.certificados-url:http://certificados-renovables-service:8103}") String certificadosUrl,
            WebClient.Builder builder) {
        this.enabled = enabled;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.tarifas = builder.clone().baseUrl(tarifasUrl).build();
        this.facturacion = builder.clone().baseUrl(facturacionUrl).build();
        this.pagos = builder.clone().baseUrl(pagosUrl).build();
        this.certificados = builder.clone().baseUrl(certificadosUrl).build();
    }

    /** Cuatro herramientas de lectura; siempre devuelve una evidencia por herramienta. */
    public List<Evidencia> consultar(AlertaTelemetria a) {
        List<Evidencia> out = new ArrayList<>();
        out.add(herramienta("tool:tarifas", "Tarifas vigentes", tarifas, "/api/v1/tarifas",
                "tarifas registradas", "El costo de la energía del activo se calcula con la tarifa vigente."));
        out.add(herramienta("tool:facturacion", "Facturación del periodo", facturacion, "/api/v1/facturas",
                "facturas emitidas", "Una falla no atendida en " + a.activoId()
                        + " puede traducirse en una factura errónea (indicador: incidentes que afectan facturación)."));
        out.add(herramienta("tool:pagos", "Pagos recientes", pagos, "/api/v1/pagos",
                "pagos registrados", "Contexto de pagos para priorizar la orden de trabajo."));
        out.add(herramienta("tool:certificados-renovables", "Certificados de energía renovable",
                certificados, "/api/v1/certificados-renovables",
                "certificados emitidos por Voltera",
                "Consulta los certificados emitidos (verdad anclada en la cadena); el agente no emite ni transfiere."));
        return out;
    }

    private Evidencia herramienta(String fuente, String titulo, WebClient cliente, String path,
                                  String unidad, String nota) {
        Integer n = enabled ? contar(cliente, path) : null;
        String detalle = n != null
                ? "Lectura MCP (solo lectura, mínimo privilegio): " + n + " " + unidad + ". " + nota
                : "Servicio no verificable en " + timeout.toMillis() + "ms (herramienta de solo lectura; "
                        + "on-premise o deshabilitada). " + nota;
        return Evidencia.de(fuente, titulo, detalle);
    }

    @SuppressWarnings("rawtypes")
    private Integer contar(WebClient cliente, String path) {
        try {
            List lista = cliente.get().uri(path).retrieve().bodyToMono(List.class)
                    .timeout(timeout).onErrorResume(e -> Mono.empty()).block();
            return lista == null ? null : lista.size();
        } catch (Exception e) {
            log.debug("MCP negocio no disponible path={} motivo={}", path, e.getMessage());
            return null;
        }
    }
}
