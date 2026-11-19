package com.voltera.blockchain.application;

import com.voltera.blockchain.domain.model.LecturaGobernanza;
import com.voltera.blockchain.domain.port.in.ConsultarGobernanzaUseCase;
import com.voltera.blockchain.domain.port.out.ContratoGobernanzaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Hash;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Lectura del estado de aprobaciones múltiples (3/3). Nunca falla: sin contrato
 * configurado, o ante un error de RPC, responde en modo SIMULADO.
 */
public class ConsultarGobernanzaService implements ConsultarGobernanzaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarGobernanzaService.class);

    private final ContratoGobernanzaPort contrato;
    private final String red;

    public ConsultarGobernanzaService(ContratoGobernanzaPort contrato, String red) {
        this.contrato = contrato;
        this.red = red;
    }

    /** propuestaId on-chain = keccak256(UTF-8 del UUID de la propuesta), en hex 0x + 64. */
    public static String aBytes32(String propuestaId) {
        byte[] h = Hash.sha3(propuestaId.getBytes(StandardCharsets.UTF_8));
        return org.web3j.utils.Numeric.toHexString(h);
    }

    @Override
    public LecturaGobernanza leer(String propuestaId) {
        if (!contrato.configurado()) {
            return LecturaGobernanza.simulada(red,
                    "Contrato multi-firma no configurado (VOLTERA_BLOCKCHAIN_RPC_URL / VOLTERA_BLOCKCHAIN_GOBERNANZA_ADDRESS)");
        }
        try {
            String id = aBytes32(propuestaId);
            Optional<ContratoGobernanzaPort.PropuestaOnChain> p = contrato.obtenerPropuesta(id);
            if (p.isEmpty()) {
                return new LecturaGobernanza(LecturaGobernanza.ON_CHAIN, false, "INEXISTENTE", 0,
                        java.util.List.of(), contrato.direccionContrato(), red,
                        "La propuesta no existe on-chain");
            }
            return new LecturaGobernanza(LecturaGobernanza.ON_CHAIN, true, p.get().estado(),
                    p.get().aprobaciones(), contrato.firmas(id), contrato.direccionContrato(), red, null);
        } catch (Exception e) {
            log.warn("Lectura on-chain falló, se responde SIMULADO: {}", e.getMessage());
            return LecturaGobernanza.simulada(red, "Lectura on-chain no disponible: " + e.getMessage());
        }
    }
}
