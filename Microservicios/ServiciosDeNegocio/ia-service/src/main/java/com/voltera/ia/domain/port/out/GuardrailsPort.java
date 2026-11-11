package com.voltera.ia.domain.port.out;

/**
 * Puerto de salida hacia los Guardrails del Model Gateway: filtros de entrada y
 * de salida (inyeccion de prompt, contenido inseguro, PII que no debe salir,
 * temas fuera de alcance). Son politicas declaradas y versionadas, no
 * instrucciones dentro del prompt.
 */
public interface GuardrailsPort {

    /**
     * Enmascara datos sensibles (PII) del texto antes de que salga hacia un
     * modelo de frontera. Es la aplicacion del PII Detector / Safety Model.
     */
    String enmascararPII(String texto);

    /**
     * Valida la entrada del usuario: rechaza (excepcion) si detecta intento de
     * inyeccion de prompt o contenido fuera de alcance.
     */
    void validarEntrada(String texto);
}
