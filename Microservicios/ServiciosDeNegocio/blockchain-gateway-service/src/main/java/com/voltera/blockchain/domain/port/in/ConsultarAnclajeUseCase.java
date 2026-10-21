package com.voltera.blockchain.domain.port.in;

import com.voltera.blockchain.domain.model.Anclaje;
import com.voltera.blockchain.domain.model.LiquidacionP2P;

import java.util.List;

/** Puerto de entrada: consulta el estado de los anclajes. */
public interface ConsultarAnclajeUseCase {
    Anclaje porTransaccion(String transaccionId);
    List<Anclaje> todos();

    /** Resultado de verificar la integridad de una liquidacion contra lo anclado. */
    record ResultadoVerificacion(
            boolean existe,
            boolean integro,
            String hashRecomputado,
            String hashAnclado,
            String txHash,
            String estado) {}

    /**
     * Recomputa el hash de la liquidacion aportada y lo compara con el hash
     * anclado para esa transaccion. Permite a un auditor verificar la integridad
     * sin confiar en el operador.
     */
    ResultadoVerificacion verificar(LiquidacionP2P liquidacion);

    /** Resumen agregado de una comunidad energetica. */
    record ResumenComunidad(
            String comunidadId,
            long totalContratos,
            long confirmados,
            long pendientes,
            long energiaWhTotal) {}

    /** Agrega los anclajes por comunidad energetica (metadato off-chain). */
    List<ResumenComunidad> resumenComunidades();
}
