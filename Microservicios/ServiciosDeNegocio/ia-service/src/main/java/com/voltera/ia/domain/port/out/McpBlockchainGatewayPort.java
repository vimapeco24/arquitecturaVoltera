package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.LecturaOnChain;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;

/**
 * MCP Blockchain Gateway (segundo MCP Server del caso). Aísla al LLM y a los
 * agentes de la complejidad on-chain del contrato {@code MultiFirmaAccionDER}:
 * expone crear propuesta, registrar firma y ejecutar la acción aprobada como
 * herramientas con identidad Zero Trust. Es un puerto de salida; el adaptador
 * puede hablar con el contrato real (vía blockchain-gateway) o funcionar en modo
 * demo determinista cuando la red no es alcanzable (fail-closed: no ejecuta nada
 * que no esté 3/3).
 *
 * <p>El resultado de cada operación es un {@code txRef} (hash/txId on-chain o un
 * identificador demo) que queda en la traza y en la auditoría.</p>
 */
public interface McpBlockchainGatewayPort {

    /** Registra la propuesta on-chain (incluye hash del diagnóstico y versión de prompt+modelo). */
    String registrarPropuesta(PropuestaAccion propuesta);

    /** Registra una firma (aprobación o rechazo) sobre una propuesta existente. */
    String registrarFirma(PropuestaAccion propuesta, Firma firma);

    /** Emite la ejecución del comando aprobado (solo si la propuesta está 3/3). */
    String ejecutarAccion(PropuestaAccion propuesta);

    /**
     * Lectura (solo lectura, sin gas) del estado de la propuesta en el contrato
     * vía blockchain-gateway. Debe devolver modo SIMULADO, sin fallar, cuando no
     * hay contrato/RPC configurados o el gateway no responde.
     */
    default LecturaOnChain leerEstado(PropuestaAccion propuesta) {
        return LecturaOnChain.simulada("lectura on-chain no disponible en este adaptador");
    }
}
