package com.voltera.ia.domain.model.gobernanza;

import java.time.Instant;

/**
 * Firma criptográfica ECDSA (secp256k1) de un rol sobre el mensaje canónico de la
 * propuesta, con prefijo EIP-191 ({@code personal_sign}). Cualquiera puede comprobarla
 * con {@code ecrecover}: la dirección recuperada debe ser la del firmante.
 *
 * @param rol       rol que firma.
 * @param direccion dirección Ethereum del firmante (0x…).
 * @param firma     firma en hex (r ‖ s ‖ v, 65 bytes).
 * @param mensaje   mensaje canónico exacto que se firmó.
 * @param metodo    CUSTODIA (clave del rol en el servicio) o BILLETERA (firmada por el usuario, p. ej. MetaMask).
 * @param firmadoEn instante de la firma.
 */
public record FirmaDigital(Firmante rol, String direccion, String firma, String mensaje,
                           String metodo, Instant firmadoEn) {
    public static final String CUSTODIA = "CUSTODIA";
    public static final String BILLETERA = "BILLETERA";
}
