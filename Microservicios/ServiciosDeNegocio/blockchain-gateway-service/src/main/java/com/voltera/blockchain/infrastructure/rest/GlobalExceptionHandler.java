package com.voltera.blockchain.infrastructure.rest;

import com.voltera.blockchain.domain.exception.AnclajeNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AnclajeNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(AnclajeNoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, Object>> invalido(Exception e) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> cadenaFallo(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(cuerpo(HttpStatus.BAD_GATEWAY, e.getMessage()));
    }

    private Map<String, Object> cuerpo(HttpStatus status, String msg) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", msg == null ? "" : msg);
    }
}
