package com.voltera.blockchain.infrastructure.blockchain;

import com.voltera.blockchain.domain.model.Anclaje;
import com.voltera.blockchain.domain.port.out.CadenaBlockchainPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de SALIDA hacia Polygon Amoy via el RPC gratuito del tercero (web3j).
 * Construye y firma la transaccion que invoca LiquidacionP2PAnchor.anclar(...).
 *
 * <p>Clasifica los errores: un revert del contrato (conflicto de hash) es
 * DEFINITIVO; un problema de red/RPC es TRANSITORIO (lo reintenta el relay).</p>
 */
public class Web3jCadenaAdapter implements CadenaBlockchainPort {

    private static final Logger log = LoggerFactory.getLogger(Web3jCadenaAdapter.class);

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final long chainId;

    public Web3jCadenaAdapter(Web3j web3j, Credentials credentials,
                              String contractAddress, long chainId) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
        this.chainId = chainId;
    }

    @Override
    public boolean disponible() {
        if (web3j == null || credentials == null
                || contractAddress == null || contractAddress.isBlank()) {
            return false;
        }
        try {
            web3j.ethBlockNumber().send();
            return true;
        } catch (Exception e) {
            log.debug("RPC no disponible: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ResultadoAnclaje anclar(Anclaje a) {
        if (!disponible()) {
            return ResultadoAnclaje.transitorio("conexion RPC no disponible");
        }
        try {
            Function fn = new Function(
                    "anclar",
                    List.of(
                            new Bytes32(txIdABytes32(a.transaccionId())),
                            new Bytes32(Numeric.hexStringToByteArray(a.hashCasacion())),
                            new Uint32(BigInteger.valueOf(a.energiaWh())),
                            new Uint64(BigInteger.valueOf(a.liquidadaEnEpoch()))
                    ),
                    List.of());
            String data = FunctionEncoder.encode(fn);

            var receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 3000, 20);
            var txManager = new RawTransactionManager(web3j, credentials, chainId, receiptProcessor);
            var gas = new DefaultGasProvider();

            EthSendTransaction sent = txManager.sendTransaction(
                    gas.getGasPrice(), gas.getGasLimit(), contractAddress, data, BigInteger.ZERO);

            if (sent.hasError()) {
                String msg = sent.getError().getMessage();
                return esDefinitivo(msg)
                        ? ResultadoAnclaje.definitivo(msg)
                        : ResultadoAnclaje.transitorio(msg);
            }

            String txHash = sent.getTransactionHash();
            Optional<TransactionReceipt> receipt =
                    receiptProcessor.waitForTransactionReceipt(txHash) == null
                            ? Optional.empty()
                            : Optional.of(receiptProcessor.waitForTransactionReceipt(txHash));

            if (receipt.isPresent() && receipt.get().isStatusOK()) {
                return ResultadoAnclaje.ok(txHash);
            }
            return ResultadoAnclaje.transitorio("recibo sin confirmacion OK (txHash=" + txHash + ")");

        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            log.warn("Error anclando transaccion={}: {}", a.transaccionId(), msg);
            return esDefinitivo(msg)
                    ? ResultadoAnclaje.definitivo(msg)
                    : ResultadoAnclaje.transitorio(msg);
        }
    }

    /** Un revert por hash en conflicto no se arregla reintentando. */
    private boolean esDefinitivo(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("revert") || m.contains("yaanclada") || m.contains("already");
    }

    /**
     * Convierte el transaccionId (String del dominio) en bytes32. Si ya viene como
     * hex 0x de 32 bytes se usa tal cual; si no, se usa su keccak256 como id en cadena.
     */
    static byte[] txIdABytes32(String transaccionId) {
        if (transaccionId.startsWith("0x") && transaccionId.length() == 66) {
            return Numeric.hexStringToByteArray(transaccionId);
        }
        byte[] hash = org.web3j.crypto.Hash.sha3(transaccionId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Arrays.copyOf(hash, 32);
    }
}
