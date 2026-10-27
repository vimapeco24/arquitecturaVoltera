package com.voltera.blockchain.infrastructure.blockchain;

import com.voltera.blockchain.domain.model.CertificadoNft;
import com.voltera.blockchain.domain.port.out.CertificadoNftPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Adaptador de SALIDA hacia el contrato ERC-721 {@code CertificadoOrigenNFT} en
 * Polygon Amoy (web3j). Invoca {@code emitirCertificado}, {@code transferFrom} y
 * {@code burn}. La cuenta del gateway debe tener EMISOR_ROLE para acuñar.
 *
 * <p>Nota: para transferir on-chain se asume que el propietario es la propia
 * cuenta del gateway (custodia). Si la custodia fuera externa, la transferencia
 * la firmaria el dueño desde su wallet; aqui modelamos la custodia por el gateway,
 * coherente con el resto del servicio.</p>
 */
public class Web3jCertificadoNftAdapter implements CertificadoNftPort {

    private static final Logger log = LoggerFactory.getLogger(Web3jCertificadoNftAdapter.class);
    private static final BigInteger GAS_PRICE_MIN = BigInteger.valueOf(30_000_000_000L);
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(350_000L);

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final long chainId;

    public Web3jCertificadoNftAdapter(Web3j web3j, Credentials credentials,
                                      String contractAddress, long chainId) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
        this.chainId = chainId;
    }

    @Override
    public boolean disponible() {
        if (web3j == null || credentials == null || contractAddress == null || contractAddress.isBlank()) {
            return false;
        }
        try {
            web3j.ethBlockNumber().send();
            return true;
        } catch (Exception e) {
            log.debug("RPC NFT no disponible: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Resultado emitir(CertificadoNft cert) {
        Function fn = new Function(
                "emitirCertificado",
                List.of(
                        new Address(cert.propietario()),
                        new Utf8String(cert.tokenUri()),
                        new Bytes32(aBytes32(cert.idMedidor())),
                        new Uint64(BigInteger.valueOf(cert.consumoNetoMensualWh())),
                        new Uint32(BigInteger.valueOf(cert.periodo())),
                        new Bytes32(Numeric.hexStringToByteArray(cert.hashDetalle()))),
                List.of());
        return enviar(fn, "emitir token medidor=" + cert.idMedidor());
    }

    @Override
    public Resultado transferir(long tokenId, String desde, String hacia) {
        Function fn = new Function(
                "transferFrom",
                List.of(new Address(desde), new Address(hacia), new Uint256(BigInteger.valueOf(tokenId))),
                List.of());
        return enviar(fn, "transferir token=" + tokenId);
    }

    @Override
    public Resultado quemar(long tokenId, String propietario) {
        Function fn = new Function(
                "burn",
                List.of(new Uint256(BigInteger.valueOf(tokenId))),
                List.of());
        return enviar(fn, "quemar token=" + tokenId);
    }

    private Resultado enviar(Function fn, String descripcion) {
        if (!disponible()) {
            return Resultado.fallo("cadena NFT no disponible");
        }
        try {
            String data = FunctionEncoder.encode(fn);
            var receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 3000, 20);
            var txManager = new RawTransactionManager(web3j, credentials, chainId, receiptProcessor);
            BigInteger gasPrice = resolverGasPrice();

            EthSendTransaction sent = txManager.sendTransaction(
                    gasPrice, GAS_LIMIT, contractAddress, data, BigInteger.ZERO);
            if (sent.hasError()) {
                return Resultado.fallo(sent.getError().getMessage());
            }
            String txHash = sent.getTransactionHash();
            TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);
            if (receipt != null && receipt.isStatusOK()) {
                Long bloque = receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null;
                log.info("[nft] {} OK txHash={}", descripcion, txHash);
                return Resultado.onChain(txHash, bloque);
            }
            return Resultado.fallo("recibo sin confirmacion OK (txHash=" + txHash + ")");
        } catch (Exception e) {
            log.warn("[nft] fallo {}: {}", descripcion, e.getMessage());
            return Resultado.fallo(String.valueOf(e.getMessage()));
        }
    }

    private BigInteger resolverGasPrice() {
        try {
            BigInteger red = web3j.ethGasPrice().send().getGasPrice();
            BigInteger conMargen = red.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
            return conMargen.max(GAS_PRICE_MIN);
        } catch (Exception e) {
            return GAS_PRICE_MIN;
        }
    }

    /** idMedidor (String) -> bytes32: si ya viene como hex 0x de 32 bytes se usa tal cual; si no, keccak256. */
    static byte[] aBytes32(String valor) {
        if (valor.startsWith("0x") && valor.length() == 66) {
            return Numeric.hexStringToByteArray(valor);
        }
        byte[] hash = Hash.sha3(valor.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(hash, 32);
    }
}
