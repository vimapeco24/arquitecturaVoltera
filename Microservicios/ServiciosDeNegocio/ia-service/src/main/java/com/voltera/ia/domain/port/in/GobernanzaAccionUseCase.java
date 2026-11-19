package com.voltera.ia.domain.port.in;

import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.gobernanza.EstadoAprobaciones;
import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.model.gobernanza.VerificacionFirma;

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

    /**
     * Vista de las aprobaciones múltiples de una propuesta: firmantes requeridos,
     * quién firmó (con tx y marca de tiempo), quién falta, estado global y modo
     * ON_CHAIN/OFF_CHAIN. Vacío si la propuesta no existe.
     */
    Optional<EstadoAprobaciones> aprobaciones(UUID propuestaId);

    /** Aprobación con firma de billetera (EIP-191) verificada con ecrecover; sin firma, firma la clave custodiada. */
    PropuestaAccion aprobar(UUID propuestaId, Firmante firmante, String identidad, String razon,
                            String direccion, String firma);

    /** Rechazo con firma de billetera (EIP-191) verificada con ecrecover; sin firma, firma la clave custodiada. */
    PropuestaAccion rechazar(UUID propuestaId, Firmante firmante, String identidad, String razon,
                             String direccion, String firma);

    /** Mensaje canónico que debe firmar el rol para aprobar o rechazar. */
    Optional<String> mensajeFirma(UUID propuestaId, Firmante rol, boolean aprueba);

    /** Verificación criptográfica (ecrecover) de cada firma registrada. */
    Optional<List<VerificacionFirma>> verificarFirmas(UUID propuestaId);
}
