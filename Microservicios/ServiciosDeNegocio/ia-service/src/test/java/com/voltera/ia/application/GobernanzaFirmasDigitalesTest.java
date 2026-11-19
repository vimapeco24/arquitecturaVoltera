package com.voltera.ia.application;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.PasoFlujo;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.der.TipoActivo;
import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.model.gobernanza.VerificacionFirma;
import com.voltera.ia.domain.port.out.McpBlockchainGatewayPort;
import com.voltera.ia.domain.port.out.NotificadorPort;
import com.voltera.ia.infrastructure.firmas.Web3jFirmaCriptografica;
import com.voltera.ia.infrastructure.persistence.InMemoryPropuestaRepository;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gobernanza 3/3 con firmas ECDSA: custodia, billetera externa, verificación y segregación de cuentas. */
class GobernanzaFirmasDigitalesTest {

    private final Web3jFirmaCriptografica cripto = new Web3jFirmaCriptografica("", "", "");

    private GobernanzaAccionService servicio() {
        McpBlockchainGatewayPort gw = new McpBlockchainGatewayPort() {
            @Override public String registrarPropuesta(PropuestaAccion p) { return "t"; }
            @Override public String registrarFirma(PropuestaAccion p, Firma f) { return "t"; }
            @Override public String ejecutarAccion(PropuestaAccion p) { return "t"; }
        };
        NotificadorPort n = new NotificadorPort() {
            @Override public String notificarFirmantePendiente(PropuestaAccion p, Firmante f) { return "n"; }
            @Override public String escalar(PropuestaAccion p, Firmante f) { return "e"; }
            @Override public String notificarDesenlace(PropuestaAccion p) { return "d"; }
        };
        return new GobernanzaAccionService(gw, n, new InMemoryPropuestaRepository(), e -> true,
                Duration.ofMinutes(30), Duration.ofMinutes(30)).conFirmas(cripto);
    }

    private DiagnosticoDer diag() {
        return DiagnosticoDer.crear("INV-1", TipoActivo.INVERSOR, "C", Severidad.CRITICA, "d",
                TipoAccion.COMANDO_FISICO, "OT", true, List.of(Evidencia.de("RAG", "t", "d")),
                List.of(PasoFlujo.de(1, "n", "p", "d")), "local", false, "diagnosticador@v3", "op");
    }

    private static String personalSign(Credentials c, String msg) {
        Sign.SignatureData s = Sign.signPrefixedMessage(msg.getBytes(StandardCharsets.UTF_8), c.getEcKeyPair());
        byte[] out = new byte[65];
        System.arraycopy(s.getR(), 0, out, 0, 32);
        System.arraycopy(s.getS(), 0, out, 32, 32);
        out[64] = s.getV()[0];
        return Numeric.toHexString(out);
    }

    @Test
    void lasTresFirmasCustodiadasSeVerificanConEcrecover() {
        var s = servicio();
        var p = s.proponer(diag());
        s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op", "ok");
        s.aprobar(p.propuestaId(), Firmante.PROSUMIDOR, "pro", "ok");
        List<VerificacionFirma> v = s.verificarFirmas(p.propuestaId()).orElseThrow();
        assertEquals(3, v.size());
        assertTrue(v.stream().allMatch(VerificacionFirma::valida));
        assertEquals(3, v.stream().map(VerificacionFirma::direccion).distinct().count());
    }

    @Test
    void firmaDeBilleteraExternaSeAceptaYQuedaComoBilletera() throws Exception {
        var s = servicio();
        var p = s.proponer(diag());
        Credentials billetera = Credentials.create(Keys.createEcKeyPair());
        String msg = s.mensajeFirma(p.propuestaId(), Firmante.OPERADOR, true).orElseThrow();
        s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op", "ok", billetera.getAddress(), personalSign(billetera, msg));
        var v = s.verificarFirmas(p.propuestaId()).orElseThrow();
        var op = v.stream().filter(x -> x.rol().equals("OPERADOR")).findFirst().orElseThrow();
        assertEquals("BILLETERA", op.metodo());
        assertTrue(op.valida());
        assertEquals(billetera.getAddress().toLowerCase(), op.direccion().toLowerCase());
    }

    @Test
    void firmaQueNoCorrespondeALaDireccionSeRechaza() throws Exception {
        var s = servicio();
        var p = s.proponer(diag());
        Credentials a = Credentials.create(Keys.createEcKeyPair());
        Credentials b = Credentials.create(Keys.createEcKeyPair());
        String msg = s.mensajeFirma(p.propuestaId(), Firmante.OPERADOR, true).orElseThrow();
        assertThrows(IllegalArgumentException.class, () ->
                s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op", "ok", b.getAddress(), personalSign(a, msg)));
        assertEquals(1, s.consultar(p.propuestaId()).orElseThrow().firmasAprobadas());
    }

    @Test
    void unaMismaCuentaNoPuedeFirmarDosRoles() throws Exception {
        var s = servicio();
        var p = s.proponer(diag());
        Credentials w = Credentials.create(Keys.createEcKeyPair());
        String m1 = s.mensajeFirma(p.propuestaId(), Firmante.OPERADOR, true).orElseThrow();
        s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op", "ok", w.getAddress(), personalSign(w, m1));
        String m2 = s.mensajeFirma(p.propuestaId(), Firmante.PROSUMIDOR, true).orElseThrow();
        assertThrows(IllegalStateException.class, () ->
                s.aprobar(p.propuestaId(), Firmante.PROSUMIDOR, "pro", "ok", w.getAddress(), personalSign(w, m2)));
        assertEquals("PENDIENTE", s.consultar(p.propuestaId()).orElseThrow().estado().name());
    }
}
