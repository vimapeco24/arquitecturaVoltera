package com.voltera.ia.infrastructure.firmas;

import com.voltera.ia.domain.model.gobernanza.Firmante;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** ECDSA secp256k1 + EIP-191: firmar, recuperar con ecrecover y detectar alteraciones. */
class Web3jFirmaCriptograficaTest {

    // Clave pública de prueba de Hardhat (cuenta #0): dirección conocida 0xf39F…2266.
    private static final String K0 = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";

    @Test
    void laDireccionRecuperadaEsLaDelFirmante() {
        var f = new Web3jFirmaCriptografica("", "", "");
        String msg = "Voltera · prueba";
        String sig = f.firmar(Firmante.OPERADOR, msg);
        assertEquals(132, sig.length());
        assertEquals(f.direccion(Firmante.OPERADOR).toLowerCase(), f.recuperarDireccion(msg, sig).toLowerCase());
        assertEquals("EFIMERAS", f.origenClaves());
    }

    @Test
    void unMensajeAlteradoRecuperaOtraDireccion() {
        var f = new Web3jFirmaCriptografica("", "", "");
        String sig = f.firmar(Firmante.PROSUMIDOR, "Decision: APRUEBA");
        assertNotEquals(f.direccion(Firmante.PROSUMIDOR).toLowerCase(),
                f.recuperarDireccion("Decision: RECHAZA", sig).toLowerCase());
    }

    @Test
    void clavesDeEntornoDanDireccionesConocidas() {
        var f = new Web3jFirmaCriptografica(K0, K0, K0);
        assertEquals("0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266", f.direccion(Firmante.AGENTE_IA).toLowerCase());
        assertEquals("ENTORNO", f.origenClaves());
    }

    @Test
    void firmaMalFormadaSeRechaza() {
        var f = new Web3jFirmaCriptografica("", "", "");
        assertThrows(IllegalArgumentException.class, () -> f.recuperarDireccion("x", "0x1234"));
    }
}
