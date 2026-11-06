package com.voltera.blockchain.infrastructure.blockchain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.math.BigInteger;
import java.util.List;

/**
 * Selecciona la billetera (Credentials) que firmará la próxima transacción.
 *
 * <p>Implementa el fallback por saldo que pidió el negocio: el gateway tiene una
 * billetera oráculo PRINCIPAL y una o varias de RESPALDO; se intenta firmar con
 * la primera que tenga saldo suficiente para pagar el gas. Si la principal se
 * quedó sin POL, se usa la siguiente, y así evitamos que un anclaje quede en
 * PENDIENTE por "insufficient funds for gas".</p>
 *
 * <p>Las credenciales llegan ya ordenadas por prioridad (principal primero). Si
 * ninguna consulta de saldo responde (RPC caído), se devuelve la primera como
 * mejor esfuerzo para no bloquear el intento.</p>
 */
public class SelectorFirmante {

    private static final Logger log = LoggerFactory.getLogger(SelectorFirmante.class);

    /**
     * Saldo mínimo para considerar una billetera "con fondos": gasLimit (350k,
     * el mayor que usamos, el del NFT) por un gas price holgado de 60 gwei.
     * 350_000 * 60 gwei = 0.021 POL. Por debajo de eso asumimos que no alcanza.
     */
    private static final BigInteger SALDO_MINIMO_WEI =
            BigInteger.valueOf(350_000L).multiply(BigInteger.valueOf(60_000_000_000L));

    private final Web3j web3j;
    private final List<Credentials> credenciales;

    public SelectorFirmante(Web3j web3j, List<Credentials> credenciales) {
        this.web3j = web3j;
        this.credenciales = credenciales;
    }

    /** Direcciones de todas las billeteras configuradas (para logs/diagnóstico). */
    public List<Credentials> credenciales() {
        return credenciales;
    }

    /**
     * Devuelve la primera credencial con saldo >= umbral. Si ninguna supera el
     * umbral pero alguna tiene saldo > 0, devuelve la de mayor saldo. Si no se
     * pudo leer ningún saldo, devuelve la principal como mejor esfuerzo. Nunca
     * devuelve null si hay al menos una credencial.
     */
    public Credentials elegir() {
        Credentials mejorParcial = null;
        BigInteger mejorSaldo = BigInteger.valueOf(-1);

        for (Credentials c : credenciales) {
            BigInteger saldo = saldoDe(c);
            if (saldo == null) {
                continue; // no se pudo leer; probar la siguiente
            }
            if (saldo.compareTo(SALDO_MINIMO_WEI) >= 0) {
                log.debug("[firmante] usando {} (saldo suficiente)", c.getAddress());
                return c;
            }
            if (saldo.compareTo(mejorSaldo) > 0) {
                mejorSaldo = saldo;
                mejorParcial = c;
            }
        }

        if (mejorParcial != null && mejorSaldo.signum() > 0) {
            log.warn("[firmante] ninguna billetera supera el umbral; usando {} con mayor saldo ({} wei)",
                    mejorParcial.getAddress(), mejorSaldo);
            return mejorParcial;
        }

        Credentials fallback = credenciales.get(0);
        log.warn("[firmante] no se pudo confirmar saldo de ninguna billetera; usando principal {} (mejor esfuerzo)",
                fallback.getAddress());
        return fallback;
    }

    private BigInteger saldoDe(Credentials c) {
        try {
            return web3j.ethGetBalance(c.getAddress(), DefaultBlockParameterName.LATEST)
                    .send().getBalance();
        } catch (Exception e) {
            log.debug("[firmante] no se pudo leer saldo de {}: {}", c.getAddress(), e.getMessage());
            return null;
        }
    }
}
