package com.voltera.ia.domain.model.gobernanza;

import java.time.Instant;

/**
 * Una firma registrada sobre una propuesta de acción. Cada firma vincula a un
 * {@link Firmante} con la identidad verificada que la emitió (IAM / Zero Trust),
 * si fue de aprobación o de rechazo, la razón (obligatoria en el rechazo) y el
 * instante. Es inmutable: refleja lo que quedó asentado on-chain.
 *
 * @param firmante  rol que firma (AGENTE_IA, OPERADOR, PROSUMIDOR).
 * @param identidad identidad verificada del firmante (dirección/usuario).
 * @param aprueba   true = aprueba; false = rechaza.
 * @param razon     justificación (recomendada al aprobar, obligatoria al rechazar).
 * @param firmadoEn marca de tiempo de la firma.
 */
public record Firma(
        Firmante firmante,
        String identidad,
        boolean aprueba,
        String razon,
        Instant firmadoEn
) {
    public static Firma aprobacion(Firmante firmante, String identidad, String razon) {
        return new Firma(firmante, identidad, true, razon == null ? "" : razon, Instant.now());
    }

    public static Firma rechazo(Firmante firmante, String identidad, String razon) {
        if (razon == null || razon.isBlank()) {
            throw new IllegalArgumentException("El rechazo exige una razón (queda on-chain).");
        }
        return new Firma(firmante, identidad, false, razon, Instant.now());
    }
}
