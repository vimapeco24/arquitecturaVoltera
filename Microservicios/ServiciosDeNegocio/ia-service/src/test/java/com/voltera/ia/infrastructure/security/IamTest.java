package com.voltera.ia.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Caja 7 · IAM: verificación de JWT y filtro con identidad tomada del token. */
@SpringBootTest(properties = {"ia.iam.enabled=true",
        "ia.iam.hs256-secret=0123456789abcdef0123456789abcdef-secreto", "eureka.client.enabled=false"})
@AutoConfigureMockMvc
class IamTest {

    static final String SECRETO = "0123456789abcdef0123456789abcdef-secreto";
    @Autowired MockMvc mvc;

    private static String b64(byte[] b) { return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }

    static String jwt(String alg, String claims, String secreto) throws Exception {
        String h = b64(("{\"alg\":\"" + alg + "\",\"typ\":\"JWT\"}").getBytes(StandardCharsets.UTF_8));
        String c = b64(claims.getBytes(StandardCharsets.UTF_8));
        String firmado = h + "." + c;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return firmado + "." + b64(mac.doFinal(firmado.getBytes(StandardCharsets.US_ASCII)));
    }

    static String claims(String user, String roles, long expDeltaSeg) {
        return "{\"preferred_username\":\"" + user + "\",\"roles\":" + roles + ",\"exp\":"
                + (Instant.now().getEpochSecond() + expDeltaSeg) + "}";
    }

    @Test
    void hs256ValidoDevuelveSujetoYRoles() throws Exception {
        var v = JwtVerificador.hs256(SECRETO, null);
        var i = v.verificar(jwt("HS256", claims("ana", "[\"operador\"]", 300), SECRETO));
        assertEquals("ana", i.sujeto());
        assertEquals(java.util.List.of("OPERADOR"), i.roles());
    }

    @Test
    void rechazaVencidoFirmaMalaYAlgoritmoNone() throws Exception {
        var v = JwtVerificador.hs256(SECRETO, null);
        assertThrows(JwtVerificador.TokenInvalido.class, () -> v.verificar(jwt("HS256", claims("a", "[]", -10), SECRETO)));
        assertThrows(JwtVerificador.TokenInvalido.class, () -> v.verificar(jwt("HS256", claims("a", "[]", 300), SECRETO + "x")));
        String none = b64("{\"alg\":\"none\"}".getBytes()) + "." + b64(claims("a", "[]", 300).getBytes()) + ".";
        assertThrows(JwtVerificador.TokenInvalido.class, () -> v.verificar(none));
        assertThrows(JwtVerificador.TokenInvalido.class, () -> JwtVerificador.hs256(SECRETO, "otro-emisor")
                .verificar(jwt("HS256", claims("a", "[]", 300), SECRETO)));
    }

    @Test
    void rs256ValidaConLaClavePublica() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        String h = b64("{\"alg\":\"RS256\"}".getBytes());
        String c = b64(claims("wso2-user", "\"PROSUMIDOR\"", 300).getBytes());
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(kp.getPrivate());
        s.update((h + "." + c).getBytes(StandardCharsets.US_ASCII));
        String token = h + "." + c + "." + b64(s.sign());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder().encodeToString(kp.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
        var i = JwtVerificador.rs256(pem, null).verificar(token);
        assertEquals("wso2-user", i.sujeto());
        assertEquals(java.util.List.of("PROSUMIDOR"), i.roles());
        assertThrows(JwtVerificador.TokenInvalido.class, () -> JwtVerificador.hs256(SECRETO, null).verificar(token));
    }

    @Test
    void sinTokenOTokenMaloDa401YElEstadoQuedaAbierto() throws Exception {
        mvc.perform(get("/api/v1/ia/agentes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/ia/agentes").header("Authorization", "Bearer basura")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/ia/estado")).andExpect(status().isOk());
    }

    @Test
    void laIdentidadDelDiagnosticoSaleDelTokenNoDelCuerpo() throws Exception {
        String t = jwt("HS256", claims("tecnico.real", "[\"OPERADOR\"]", 300), SECRETO);
        mvc.perform(post("/api/v1/ia/diagnostico/der").header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activoId\":\"INV-IAM\",\"tipoActivo\":\"INVERSOR\",\"comunidadId\":\"C\",\"metrica\":\"voltaje\","
                                + "\"valor\":265,\"umbral\":240,\"minutosSinSenal\":0,\"usuario\":\"suplantado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("tecnico.real"));
    }

    @Test
    void firmarExigeElRolDelFirmante() throws Exception {
        String t = jwt("HS256", claims("tecnico.real", "[\"OPERADOR\"]", 300), SECRETO);
        mvc.perform(post("/api/v1/ia/gobernanza/propuestas/" + java.util.UUID.randomUUID() + "/firmas/prosumidor")
                        .header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identidad\":\"x\",\"razon\":\"r\"}"))
                .andExpect(status().isForbidden());
    }
}
