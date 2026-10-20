package com.voltera.blockchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de borde "blockchain-gateway" de Voltera.
 *
 * <p>Zona de BORDE de la arquitectura DApp (ver BLOCKCHAIN_VOLTERA.md §6):
 * <ul>
 *   <li><b>Oraculo</b>: recibe liquidaciones P2P del dominio, computa el hash
 *       canonico y lo ancla en el contrato LiquidacionP2PAnchor de Polygon Amoy
 *       a traves de un RPC gratuito de tercero (web3j JSON-RPC).</li>
 *   <li><b>Indexador</b>: hace polling de los eventos LiquidacionAnclada emitidos
 *       por el contrato y los republica como evento de dominio en Kafka/Redpanda.</li>
 *   <li><b>OUTBOX</b>: el anclaje se persiste primero en una bandeja transaccional;
 *       un relay programado reintenta hasta confirmar on-chain (gate 6: la cadena
 *       queda fuera del camino critico, la confirmacion vuelve por evento).</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class BlockchainGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlockchainGatewayApplication.class, args);
    }
}
