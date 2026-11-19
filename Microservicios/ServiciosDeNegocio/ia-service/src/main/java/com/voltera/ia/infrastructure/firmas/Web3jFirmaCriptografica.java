package com.voltera.ia.infrastructure.firmas;

import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.port.out.FirmaCriptograficaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * Firmas ECDSA secp256k1 (EIP-191) con web3j. Cada rol tiene su propia clave:
 * se toma de {@code IA_FIRMA_KEY_AGENTE_IA}, {@code IA_FIRMA_KEY_OPERADOR} y
 * {@code IA_FIRMA_KEY_PROSUMIDOR}; si faltan, se generan al arrancar (claves efímeras,
 * se registran solo las direcciones, nunca las claves).
 */
@Component
public class Web3jFirmaCriptografica implements FirmaCriptograficaPort {

    private static final Logger log = LoggerFactory.getLogger(Web3jFirmaCriptografica.class);
    private final Map<Firmante, Credentials> claves = new EnumMap<>(Firmante.class);
    private final String origen;

    public Web3jFirmaCriptografica(
            @Value("${ia.firmas.clave-agente:}") String kAgente,
            @Value("${ia.firmas.clave-operador:}") String kOperador,
            @Value("${ia.firmas.clave-prosumidor:}") String kProsumidor) {
        boolean entorno = !kAgente.isBlank() && !kOperador.isBlank() && !kProsumidor.isBlank();
        claves.put(Firmante.AGENTE_IA, cargar(kAgente));
        claves.put(Firmante.OPERADOR, cargar(kOperador));
        claves.put(Firmante.PROSUMIDOR, cargar(kProsumidor));
        origen = entorno ? "ENTORNO" : "EFIMERAS";
        claves.forEach((r, c) -> log.info("[firmas] rol={} direccion={} origen={}", r, c.getAddress(), origen));
    }

    private static Credentials cargar(String hex) {
        try {
            if (hex != null && !hex.isBlank()) return Credentials.create(hex.trim());
            return Credentials.create(Keys.createEcKeyPair());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear la clave de firma", e);
        }
    }

    @Override
    public String firmar(Firmante rol, String mensaje) {
        ECKeyPair kp = claves.get(rol).getEcKeyPair();
        Sign.SignatureData s = Sign.signPrefixedMessage(mensaje.getBytes(StandardCharsets.UTF_8), kp);
        byte[] out = new byte[65];
        System.arraycopy(s.getR(), 0, out, 0, 32);
        System.arraycopy(s.getS(), 0, out, 32, 32);
        out[64] = s.getV()[0];
        return Numeric.toHexString(out);
    }

    @Override
    public String direccion(Firmante rol) {
        return claves.get(rol).getAddress();
    }

    @Override
    public String recuperarDireccion(String mensaje, String firmaHex) {
        byte[] b = Numeric.hexStringToByteArray(firmaHex == null ? "" : firmaHex);
        if (b.length != 65) throw new IllegalArgumentException("Firma inválida: se esperaban 65 bytes.");
        byte v = b[64];
        if (v < 27) v += 27;
        Sign.SignatureData sd = new Sign.SignatureData(v, Arrays.copyOfRange(b, 0, 32), Arrays.copyOfRange(b, 32, 64));
        try {
            BigInteger pub = Sign.signedPrefixedMessageToKey(mensaje.getBytes(StandardCharsets.UTF_8), sd);
            return "0x" + Keys.getAddress(pub);
        } catch (Exception e) {
            throw new IllegalArgumentException("Firma inválida: " + e.getMessage());
        }
    }

    @Override
    public String origenClaves() {
        return origen;
    }
}
