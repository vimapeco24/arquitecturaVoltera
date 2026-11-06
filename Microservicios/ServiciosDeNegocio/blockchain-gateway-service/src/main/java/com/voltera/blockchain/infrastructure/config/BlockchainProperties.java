package com.voltera.blockchain.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de conexion a la cadena. Se rellenan desde variables de entorno
 * VOLTERA_BLOCKCHAIN_* / application.yml.
 */
@ConfigurationProperties(prefix = "voltera.blockchain")
public class BlockchainProperties {

    /** URL del RPC del tercero (p. ej. https://rpc-amoy.polygon.technology). */
    private String rpcUrl = "";
    /** Clave privada de la cuenta oraculo PRINCIPAL (solo testnet). */
    private String privateKey = "";
    /**
     * Claves privadas de cuentas de RESPALDO (solo testnet), separadas por coma.
     * Se definen con VOLTERA_BLOCKCHAIN_PRIVATE_KEYS. El gateway intenta firmar
     * con la principal; si esa billetera no tiene saldo para el gas, prueba la
     * siguiente de esta lista (fallback por saldo).
     */
    private String privateKeys = "";
    /** Direccion del contrato LiquidacionP2PAnchor desplegado. */
    private String contractAddress = "";
    /** Direccion del contrato CertificadoOrigenNFT (ERC-721) desplegado. */
    private String certificadoNftAddress = "";
    /** chainId de la red (Amoy = 80002). */
    private long chainId = 80002L;
    /** Nombre logico de la red para los eventos. */
    private String network = "polygon-amoy";

    public boolean estaConfigurada() {
        return !isBlank(rpcUrl) && !getTodasLasClaves().isEmpty() && !isBlank(contractAddress);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Quita espacios, tabuladores y saltos de línea que a veces se cuelan al
     * pegar secrets en paneles de PaaS (Render/Fly). Sin esto, una dirección o
     * clave con un '\n' hace fallar web3j (NumberFormatException / dirección
     * inválida) y el servicio no arranca o los anclajes revientan.
     */
    private static String limpiar(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "");
    }

    public String getRpcUrl() { return rpcUrl; }
    public void setRpcUrl(String rpcUrl) { this.rpcUrl = limpiar(rpcUrl); }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = limpiar(privateKey); }
    public String getPrivateKeys() { return privateKeys; }
    public void setPrivateKeys(String privateKeys) { this.privateKeys = privateKeys == null ? "" : privateKeys; }

    /**
     * Lista ordenada de claves privadas a usar para firmar: primero la principal
     * ({@code privateKey}) y luego las de respaldo ({@code privateKeys}, separadas
     * por coma). Cada clave queda normalizada (sin espacios, con prefijo 0x) y se
     * omiten duplicados y vacias. El orden define la prioridad del fallback.
     */
    public List<String> getTodasLasClaves() {
        List<String> claves = new ArrayList<>();
        agregarClave(claves, privateKey);
        if (privateKeys != null && !privateKeys.isBlank()) {
            for (String k : privateKeys.split(",")) {
                agregarClave(claves, k);
            }
        }
        return claves;
    }

    private static void agregarClave(List<String> acc, String raw) {
        String norm = normalizarClave(raw);
        if (!norm.isEmpty() && !acc.contains(norm)) {
            acc.add(norm);
        }
    }

    /** Limpia espacios/saltos y garantiza el prefijo 0x de una clave privada. */
    public static String normalizarClave(String pk) {
        if (pk == null) return "";
        String limpio = pk.replaceAll("\\s+", "");
        if (limpio.isEmpty()) return "";
        return limpio.startsWith("0x") ? limpio : "0x" + limpio;
    }
    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = limpiar(contractAddress); }
    public String getCertificadoNftAddress() { return certificadoNftAddress; }
    public void setCertificadoNftAddress(String certificadoNftAddress) { this.certificadoNftAddress = limpiar(certificadoNftAddress); }
    public long getChainId() { return chainId; }
    public void setChainId(long chainId) { this.chainId = chainId; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = limpiar(network); }
}
