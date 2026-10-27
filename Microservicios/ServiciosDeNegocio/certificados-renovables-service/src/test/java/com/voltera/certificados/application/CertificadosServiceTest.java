package com.voltera.certificados.application;

import com.voltera.certificados.domain.model.CertificadoRenovable;
import com.voltera.certificados.domain.port.in.GestionarCertificadosUseCase.ComandoEmitir;
import com.voltera.certificados.infrastructure.gateway.NoopGatewayCadenaAdapter;
import com.voltera.certificados.infrastructure.persistence.InMemoryCertificadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CertificadosService — emisión/titularidad/transferencia/retiro vía gateway (demo)")
class CertificadosServiceTest {

    private static final String EMISOR = "0xA0A1";
    private static final String PROP = "0x1111";
    private static final String OTRO = "0x2222";

    private CertificadosService service;

    @BeforeEach
    void setUp() {
        service = new CertificadosService(new InMemoryCertificadoRepository(), new NoopGatewayCadenaAdapter());
    }

    private ComandoEmitir cmd() {
        return new ComandoEmitir("MED-1", EMISOR, PROP, 120_000L, 202610, "SOL-1");
    }

    @Test
    @DisplayName("emite y el gateway confirma: queda CONFIRMADO con tokenId y txHash")
    void emiteYconfirma() {
        CertificadoRenovable c = service.emitir(cmd());
        assertEquals(CertificadoRenovable.EstadoCadena.CONFIRMADO, c.estadoCadena());
        assertEquals(CertificadoRenovable.EstadoDominio.EMITIDO, c.estadoDominio());
        assertNotNull(c.tokenId());
        assertNotNull(c.txHash());
        assertEquals(1, c.historial().size());
        assertEquals("EMISION", c.historial().get(0).tipo());
        assertEquals(CertificadoRenovable.ZERO, c.historial().get(0).desde());
        assertEquals(PROP, c.historial().get(0).hacia());
    }

    @Test
    @DisplayName("transfiere la titularidad de una billetera a otra (queda en el historial)")
    void transfiere() {
        CertificadoRenovable c = service.emitir(cmd());
        CertificadoRenovable t = service.transferir(c.id(), OTRO);
        assertEquals(OTRO, t.titular());
        assertEquals("TRANSFERENCIA", t.historial().get(t.historial().size() - 1).tipo());
        assertEquals(PROP, t.historial().get(t.historial().size() - 1).desde());
        assertEquals(OTRO, t.historial().get(t.historial().size() - 1).hacia());
    }

    @Test
    @DisplayName("no transfiere al mismo titular ni a dirección cero")
    void noTransfiereInvalido() {
        CertificadoRenovable c = service.emitir(cmd());
        assertThrows(IllegalArgumentException.class, () -> service.transferir(c.id(), PROP));
        assertThrows(IllegalArgumentException.class, () -> service.transferir(c.id(), CertificadoRenovable.ZERO));
    }

    @Test
    @DisplayName("retira el certificado y no permite transferirlo después")
    void retira() {
        CertificadoRenovable c = service.emitir(cmd());
        CertificadoRenovable r = service.retirar(c.id());
        assertEquals(CertificadoRenovable.EstadoDominio.RETIRADO, r.estadoDominio());
        assertThrows(IllegalStateException.class, () -> service.transferir(c.id(), OTRO));
    }

    @Test
    @DisplayName("filtra por titular actual")
    void filtraPorTitular() {
        CertificadoRenovable c1 = service.emitir(cmd());
        service.emitir(new ComandoEmitir("MED-2", EMISOR, PROP, 50_000L, 202610, "SOL-2"));
        service.transferir(c1.id(), OTRO);
        assertEquals(1, service.porTitular(OTRO).size());
        assertEquals(1, service.porTitular(PROP).size());
        assertEquals(2, service.todos().size());
    }
}
