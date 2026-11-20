package com.voltera.ia.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Verificación de JWT (Caja 7 · Identity/IAM) solo con el JDK: HS256 (secreto compartido) o RS256 (clave
 * pública PEM del proveedor, p. ej. WSO2). Valida firma, expiración y, si se configura, el emisor.
 * No acepta {@code alg=none} ni un algoritmo distinto del configurado.
 */
public class JwtVerificador {

    public record Identidad(String sujeto, List<String> roles) {}

    public static class TokenInvalido extends RuntimeException {
        public TokenInvalido(String m) { super(m); }
    }

    private final String algoritmo; // HS256 | RS256
    private final byte[] secreto;
    private final PublicKey clavePublica;
    private final String emisor;
    private final ObjectMapper mapper = new ObjectMapper();

    private JwtVerificador(String algoritmo, byte[] secreto, PublicKey clavePublica, String emisor) {
        this.algoritmo = algoritmo;
        this.secreto = secreto;
        this.clavePublica = clavePublica;
        this.emisor = emisor;
    }

    public static JwtVerificador hs256(String secreto, String emisor) {
        if (secreto == null || secreto.length() < 32) throw new IllegalArgumentException("el secreto HS256 debe tener al menos 32 caracteres");
        return new JwtVerificador("HS256", secreto.getBytes(StandardCharsets.UTF_8), null, emisor);
    }

    public static JwtVerificador rs256(String pem, String emisor) {
        try {
            String b64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
            PublicKey k = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(b64)));
            return new JwtVerificador("RS256", null, k, emisor);
        } catch (Exception e) {
            throw new IllegalArgumentException("clave pública RS256 inválida", e);
        }
    }

    public Identidad verificar(String token) {
        try {
            String[] p = token.split("\\.");
            if (p.length != 3) throw new TokenInvalido("formato");
            JsonNode h = mapper.readTree(Base64.getUrlDecoder().decode(p[0]));
            if (!algoritmo.equals(h.path("alg").asText())) throw new TokenInvalido("algoritmo no permitido");
            byte[] firmado = (p[0] + "." + p[1]).getBytes(StandardCharsets.US_ASCII);
            byte[] firma = Base64.getUrlDecoder().decode(p[2]);
            if (!firmaValida(firmado, firma)) throw new TokenInvalido("firma");
            JsonNode c = mapper.readTree(Base64.getUrlDecoder().decode(p[1]));
            if (!c.hasNonNull("exp") || Instant.now().getEpochSecond() >= c.get("exp").asLong()) throw new TokenInvalido("vencido");
            if (emisor != null && !emisor.isBlank() && !emisor.equals(c.path("iss").asText())) throw new TokenInvalido("emisor");
            String sujeto = c.hasNonNull("preferred_username") ? c.get("preferred_username").asText() : c.path("sub").asText("");
            if (sujeto.isBlank()) throw new TokenInvalido("sin sujeto");
            List<String> roles = new ArrayList<>();
            JsonNode r = c.get("roles");
            if (r != null && r.isArray()) r.forEach(x -> roles.add(x.asText().toUpperCase()));
            else if (r != null && r.isTextual()) for (String x : r.asText().split("[ ,]+")) roles.add(x.toUpperCase());
            return new Identidad(sujeto, roles);
        } catch (TokenInvalido e) {
            throw e;
        } catch (Exception e) {
            throw new TokenInvalido("ilegible");
        }
    }

    private boolean firmaValida(byte[] datos, byte[] firma) throws Exception {
        if ("HS256".equals(algoritmo)) {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return MessageDigest.isEqual(mac.doFinal(datos), firma);
        }
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(clavePublica);
        s.update(datos);
        return s.verify(firma);
    }
}
