package com.voltera.blockchain.domain.port.out;

import com.voltera.blockchain.domain.model.LecturaGobernanza;

import java.util.List;
import java.util.Optional;

/** Puerto de SALIDA (solo lectura) hacia el contrato {@code MultiFirmaAccionDER}. */
public interface ContratoGobernanzaPort {

    /** true si hay RPC y dirección de contrato configurados. */
    boolean configurado();

    /** Dirección del contrato consultado (o null). */
    String direccionContrato();

    /** Estado crudo de la propuesta (empty si no existe on-chain). */
    Optional<PropuestaOnChain> obtenerPropuesta(String propuestaIdBytes32);

    /** Firmas registradas (eventos FirmaRegistrada) de la propuesta. */
    List<LecturaGobernanza.FirmaLeida> firmas(String propuestaIdBytes32);

    /** Estado on-chain: estado es el nombre del enum Estado del contrato. */
    record PropuestaOnChain(String estado, int aprobaciones) { }
}
