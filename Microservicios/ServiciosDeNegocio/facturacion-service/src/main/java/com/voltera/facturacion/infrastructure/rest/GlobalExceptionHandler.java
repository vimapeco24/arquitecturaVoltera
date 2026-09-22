package com.voltera.facturacion.infrastructure.rest;

import com.voltera.facturacion.domain.exception.FacturaNoEncontradaException;
import com.voltera.facturacion.domain.exception.TarifaNoDisponibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Traduce excepciones de dominio a respuestas HTTP coherentes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FacturaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> noEncontrada(FacturaNoEncontradaException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Circuit breaker abierto / tarifas caído -> 503 fail-fast. */
    @ExceptionHandler(TarifaNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> tarifasNoDisponible(TarifaNoDisponibleException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> reglaDeNegocio(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Datos invalidos");
        return build(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
