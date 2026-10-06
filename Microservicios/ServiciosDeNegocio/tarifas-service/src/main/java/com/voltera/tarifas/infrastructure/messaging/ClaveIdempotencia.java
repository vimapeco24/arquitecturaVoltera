package com.voltera.tarifas.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Deriva la CLAVE DE IDEMPOTENCIA de un evento (lamina 10): usa {@code eventId} si
 * el payload lo trae; si no, compone {@code tipoEvento|clave|timestamp} con la
 * clave de negocio (medidorId/serial) y el campo temporal disponible. Como ultimo
 * recurso usa el hash del payload para no colisionar entre eventos distintos.
 */
final class ClaveIdempotencia {

    private static final String[] CLAVES_NEGOCIO = {"medidorId", "medidorSerial", "serial"};
    private static final String[] CAMPOS_TIEMPO = {
            "timestamp", "capturadaEn", "validadaEn", "creadoEn",
            "finIntervalo", "inicioIntervalo", "detectadoEn", "habilitadoEn", "cerradaEn"
    };

    private ClaveIdempotencia() {}

    static String derivar(JsonNode n, String tipoEvento) {
        if (n.hasNonNull("eventId")) {
            return n.get("eventId").asText();
        }
        String claveNegocio = primero(n, CLAVES_NEGOCIO);
        String tiempo = primero(n, CAMPOS_TIEMPO);
        if (claveNegocio != null && tiempo != null) {
            return tipoEvento + "|" + claveNegocio + "|" + tiempo;
        }
        // Ultimo recurso: contenido del evento (estable para el mismo payload).
        return tipoEvento + "|" + n.toString().hashCode();
    }

    private static String primero(JsonNode n, String... campos) {
        for (String c : campos) {
            if (n.hasNonNull(c)) return n.get(c).asText();
        }
        return null;
    }
}
