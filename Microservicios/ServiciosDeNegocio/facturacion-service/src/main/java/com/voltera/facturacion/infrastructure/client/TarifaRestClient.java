package com.voltera.facturacion.infrastructure.client;

import com.voltera.facturacion.domain.exception.TarifaNoDisponibleException;
import com.voltera.facturacion.domain.port.out.TarifaConsultaPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Adaptador de SALIDA (driven adapter): implementa el puerto TarifaConsultaPort
 * realizando la LLAMADA REAL ENTRE MICROSERVICIOS  facturacion-service -> tarifas-service.
 *
 * Se usa RestClient (síncrono, Spring 6.1+). Al estar en el classpath
 * micrometer-tracing, RestClient queda instrumentado automáticamente: propaga
 * las cabeceras de traza (b3/traceparent) y genera un span cliente, de modo que
 * el salto aparece como una arista continua en Kiali/Jaeger y su latencia se
 * mide en Prometheus (istio_request_duration_milliseconds).
 *
 * RESILIENCIA (Circuit Breaker de aplicación, Resilience4j):
 *   - @Retry("tarifas")          reintenta fallos transitorios.
 *   - @CircuitBreaker("tarifas") si tarifas falla de forma sostenida, ABRE el
 *     breaker y deja de llamar (fail-fast) durante wait-duration; ejecuta el
 *     fallback. Esto complementa el circuit breaker de MALLA (Istio) que además
 *     protege a nivel de red para los 6 servicios.
 *   El estado (CLOSED/OPEN/HALF_OPEN) se expone en /actuator/circuitbreakers.
 */
@Component
public class TarifaRestClient implements TarifaConsultaPort {

    private final RestClient rest;

    public TarifaRestClient(RestClient.Builder builder,
                            @Value("${tarifas.base-url:http://tarifas-service:8084}") String baseUrl) {
        this.rest = builder.baseUrl(baseUrl).build();
    }

    @Override
    @Retry(name = "tarifas")
    @CircuitBreaker(name = "tarifas", fallbackMethod = "precioFallback")
    public BigDecimal obtenerPrecioKwh(String tarifaId, int hora) {
        Map<String, Object> resp = rest.get()
                .uri("/api/v1/tarifas/{id}/precio?hora={h}", tarifaId, hora)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (resp == null || resp.get("precioKwh") == null) {
            throw new IllegalStateException(
                    "Respuesta inválida de tarifas-service para tarifa=" + tarifaId + " hora=" + hora);
        }
        return new BigDecimal(resp.get("precioKwh").toString());
    }

    /**
     * Fallback del Circuit Breaker: se ejecuta cuando el breaker está ABIERTO o
     * la llamada falla tras los reintentos. Lanza una excepción de dominio que el
     * borde REST traduce a HTTP 503 (fail-fast, sin colgar la petición).
     */
    @SuppressWarnings("unused")
    private BigDecimal precioFallback(String tarifaId, int hora, Throwable ex) {
        throw new TarifaNoDisponibleException(tarifaId, ex);
    }
}
