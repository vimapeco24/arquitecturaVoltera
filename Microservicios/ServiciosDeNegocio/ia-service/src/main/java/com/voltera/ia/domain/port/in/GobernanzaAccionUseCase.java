package com.voltera.ia.domain.port.in;

import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso de la gobernanza multi-firma 3/3 sobre acciones correctivas de
 * dispositivos DER. Expone el ciclo del documento: el Agente de Acción crea la
 * propuesta (Firma 1), el operador y el prosumidor firman o rechazan (Firmas 2 y
 * 3), y solo con 3/3 se ejecuta la acción. El Agente Notificador avisa y escala.
 */
public interface GobernanzaAccionUseCase {

    /**
     * Crea una propuesta de acción a partir de un diagnóstico que requiere
     * intervención, aplica la Firma 1 del agente, la registra on-chain vía el MCP
     * Blockchain Gateway y notifica al operador (siguiente firmante).
     */
    PropuestaAccion proponer(DiagnosticoDer diagnostico);

    /** Registra la aprobación de un firmante humano (operador o prosumidor). */
    PropuestaAccion aprobar(UUID propuestaId, Firmante firmante, String identidad, String razon);

    /** Registra el rechazo de un firmante (razón obligatoria, queda on-chain). */
    PropuestaAccion rechazar(UUID propuestaId, Firmante firmante, String identidad, String razon);

    /** Marca vencidas las propuestas cuyo TTL expiró sin completar las 3 firmas. */
    List<PropuestaAccion> caducarVencidas();

    Optional<PropuestaAccion> consultar(UUID propuestaId);

    List<PropuestaAccion> listar();
}
