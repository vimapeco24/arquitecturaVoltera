package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.gobernanza.Firmante;

import java.util.Map;

/**
 * Firma y verificación ECDSA secp256k1 con prefijo EIP-191 (compatible con
 * {@code personal_sign} de MetaMask y con {@code ecrecover} on-chain).
 */
public interface FirmaCriptograficaPort {

    /** Firma el mensaje con la clave custodiada del rol. Devuelve la firma en hex (65 bytes). */
    String firmar(Firmante rol, String mensaje);

    /** Dirección (0x…) asociada a la clave custodiada del rol. */
    String direccion(Firmante rol);

    /** Recupera la dirección que firmó el mensaje (ecrecover). Lanza IllegalArgumentException si la firma es inválida. */
    String recuperarDireccion(String mensaje, String firmaHex);

    /** Origen de las claves custodiadas: ENTORNO (variables de entorno) o EFIMERAS (generadas al arrancar). */
    String origenClaves();

    default Map<Firmante, String> direcciones() {
        Map<Firmante, String> m = new java.util.EnumMap<>(Firmante.class);
        for (Firmante f : Firmante.values()) m.put(f, direccion(f));
        return m;
    }
}
