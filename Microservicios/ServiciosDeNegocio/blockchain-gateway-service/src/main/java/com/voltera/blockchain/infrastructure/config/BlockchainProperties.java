package com.voltera.blockchain.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de conexion a la cadena. Se rellenan desde variables de entorno
 * VOLTERA_BLOCKCHAIN_* / application.yml.
 */
@ConfigurationProperties(prefix = "voltera.blockchain")
public class BlockchainProperties {

    /** URL del RPC del tercero (p. ej. https://rpc-amoy.polygon.technology). */
    private String rpcUrl = "";
    /** Clave privada de la cuenta oraculo (solo testnet). */
    private String privateKey = "";
    /** Direccion del contrato LiquidacionP2PAnchor desplegado. */
    private String contractAddress = "";
    /** chainId de la red (Amoy = 80002). */
    private long chainId = 80002L;
    /** Nombre logico de la red para los eventos. */
    private String network = "polygon-amoy";

    public boolean estaConfigurada() {
        return !isBlank(rpcUrl) && !isBlank(privateKey) && !isBlank(contractAddress);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public String getRpcUrl() { return rpcUrl; }
    public void setRpcUrl(String rpcUrl) { this.rpcUrl = rpcUrl; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }
    public long getChainId() { return chainId; }
    public void setChainId(long chainId) { this.chainId = chainId; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
}
