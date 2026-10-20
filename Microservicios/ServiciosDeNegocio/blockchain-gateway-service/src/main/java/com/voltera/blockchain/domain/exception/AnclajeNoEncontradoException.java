package com.voltera.blockchain.domain.exception;

public class AnclajeNoEncontradoException extends RuntimeException {
    public AnclajeNoEncontradoException(String transaccionId) {
        super("No existe anclaje para la transaccion: " + transaccionId);
    }
}
