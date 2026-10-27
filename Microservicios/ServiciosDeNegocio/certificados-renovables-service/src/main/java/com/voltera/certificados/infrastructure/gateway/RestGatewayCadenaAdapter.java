package com.voltera.certificados.infrastructure.gateway;

import com.voltera.certificados.domain.model.CertificadoRenovable;
import com.voltera.certificados.domain.port.out.GatewayCadenaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Adaptador de SALIDA + <b>Anticorruption Layer</b> hacia el blockchain-gateway.
 * Traduce el agregado de dominio {@code CertificadoRenovable} al contrato REST
 * del gateway ({@code /api/v1/certificados}) y de vuelta. Es la frontera: aquí
 * (y solo aquí) el dominio de Certificados Renovables "habla" con el mundo de la
 * cadena, SIN acoplarse a web3j ni a Solidity.
 */
public class RestGatewayCadenaAdapter implements GatewayCadenaPort {

    private static final Logger log = LoggerFactory.getLogger(RestGatewayCadenaAdapter.class);

    private final RestClient client;
    private final String basePath;

    public RestGatewayCadenaAdapter(String baseUrl, String certificadosPath) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.basePath = certificadosPath;
    }

    @Override
    public boolean configurado() {
        return true;
    }

    @Override
    public Resultado emitir(CertificadoRenovable cert) {
        // ACL: modelo de dominio -> contrato del gateway (EmitirCertificadoRequest).
        Map<String, Object> body = Map.of(
                "emisor", cert.emisor(),
                "propietario", cert.titular(),
                "idMedidor", cert.idMedidor(),
                "consumoNetoMensualWh", cert.consumoNetoMensualWh(),
                "periodo", cert.periodo());
        try {
            Map<?, ?> resp = client.post().uri(basePath).body(body)
                    .retrieve().body(Map.class);
            Long tokenId = extraerTokenId(resp);
            String txHash = extraerTxHash(resp);
            log.info("Gateway emitió NFT certificado={} tokenId={} txHash={}", cert.id(), tokenId, txHash);
            return Resultado.ok(tokenId, txHash);
        } catch (Exception e) {
            log.warn("Gateway no pudo emitir certificado={}: {}", cert.id(), e.getMessage());
            return Resultado.fallo(e.getMessage());
        }
    }

    @Override
    public Resultado transferir(long tokenId, String desde, String hacia) {
        try {
            Map<?, ?> resp = client.post().uri(basePath + "/" + tokenId + "/transferir")
                    .body(Map.of("hacia", hacia))
                    .retrieve().body(Map.class);
            return Resultado.ok(tokenId, extraerTxHash(resp));
        } catch (Exception e) {
            log.warn("Gateway no pudo transferir tokenId={}: {}", tokenId, e.getMessage());
            return Resultado.fallo(e.getMessage());
        }
    }

    @Override
    public Resultado retirar(long tokenId, String titular) {
        try {
            Map<?, ?> resp = client.post().uri(basePath + "/" + tokenId + "/quemar")
                    .retrieve().body(Map.class);
            return Resultado.ok(tokenId, extraerTxHash(resp));
        } catch (Exception e) {
            log.warn("Gateway no pudo retirar tokenId={}: {}", tokenId, e.getMessage());
            return Resultado.fallo(e.getMessage());
        }
    }

    private static Long extraerTokenId(Map<?, ?> resp) {
        if (resp == null) return null;
        Object v = resp.get("tokenId");
        if (v instanceof Number n) return n.longValue();
        return null;
    }

    private static String extraerTxHash(Map<?, ?> resp) {
        if (resp == null) return null;
        Object hist = resp.get("historial");
        // El gateway devuelve el txHash dentro del último movimiento del historial.
        if (hist instanceof java.util.List<?> lista && !lista.isEmpty()) {
            Object ultimo = lista.get(lista.size() - 1);
            if (ultimo instanceof Map<?, ?> m && m.get("txHash") != null) {
                return String.valueOf(m.get("txHash"));
            }
        }
        Object tx = resp.get("txHash");
        return tx == null ? null : String.valueOf(tx);
    }
}
