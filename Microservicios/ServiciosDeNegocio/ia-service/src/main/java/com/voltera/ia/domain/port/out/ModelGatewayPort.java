package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.SolicitudCopiloto;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida hacia el Model Gateway: unico punto por el que pasa toda
 * llamada a un LLM. Aplica ruteo por clase de dato, enmascaramiento, presupuesto
 * de tokens, timeouts y respaldo. Si no hay proveedor configurado, devuelve
 * Optional.empty() y el agente razona con el motor local heuristico (no se saca
 * ningun dato del clúster: Zero-retention por defecto).
 */
public interface ModelGatewayPort {

    /**
     * Pide al LLM una redaccion/justificacion a partir de la evidencia ya
     * recuperada y enmascarada. Nunca recibe PII sin enmascarar.
     *
     * @return la justificacion generada, o vacio si el gateway no tiene proveedor
     *         activo o la llamada falla (el flujo cae al motor local).
     */
    Optional<String> redactarJustificacion(SolicitudCopiloto solicitud, List<Evidencia> evidencias);

    /** true si hay un proveedor de LLM configurado y activo. */
    boolean proveedorActivo();

    /** Identificador del motor para auditoria: "llm:<modelo>" o "local". */
    String identificadorMotor();
}
