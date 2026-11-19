package com.voltera.ia.domain.model.gobernanza;

import java.time.Instant;

/**
 * Resultado de verificar una firma con ecrecover: se reconstruye el mensaje canónico
 * a partir del estado actual de la propuesta y se recupera la dirección firmante.
 * {@code valida} es true solo si coincide con la dirección registrada.
 */
public record VerificacionFirma(String rol, String direccion, String direccionRecuperada,
                                String firma, String metodo, String mensaje, boolean valida,
                                Instant firmadoEn, String detalle) {}
