package com.voltera.certificacion.application;

import com.voltera.certificacion.domain.model.BalanceGeneracionCertificable;
import com.voltera.certificacion.domain.model.SolicitudCertificacion;
import com.voltera.certificacion.domain.port.in.CertificarGeneracionUseCase.ComandoRegistrarGeneracion;
import com.voltera.certificacion.domain.port.in.CertificarGeneracionUseCase.ComandoSolicitar;
import com.voltera.certificacion.infrastructure.persistence.InMemorySolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CertificacionService — bloques de 1 MWh sin reutilizar mediciones + doble aval")
class CertificacionServiceTest {

    private static final long BLOQUE = 1_000_000L; // 1 MWh
    private CertificacionService service;

    @BeforeEach
    void setUp() {
        service = new CertificacionService(new InMemorySolicitudRepository(), BLOQUE);
    }

    @Test
    @DisplayName("acumula generación y NO reutiliza una medición ya contabilizada")
    void noReutilizaMediciones() {
        service.registrarGeneracion(new ComandoRegistrarGeneracion("MED-1", "m1", 600_000));
        service.registrarGeneracion(new ComandoRegistrarGeneracion("MED-1", "m1", 600_000)); // duplicada
        BalanceGeneracionCertificable b = service.balanceDe("MED-1");
        assertEquals(600_000, b.generacionAcumuladaWh());
        assertEquals(1, b.medicionesContabilizadas());
        assertFalse(b.puedeReservarBloque());
    }

    @Test
    @DisplayName("al alcanzar 1 MWh se puede reservar un bloque y abrir una solicitud PENDIENTE")
    void reservaBloqueYabreSolicitud() {
        service.registrarGeneracion(new ComandoRegistrarGeneracion("MED-1", "m1", 700_000));
        service.registrarGeneracion(new ComandoRegistrarGeneracion("MED-1", "m2", 400_000));
        SolicitudCertificacion s = service.solicitarCertificacion(new ComandoSolicitar("MED-1", "0xPROP", 202610));

        assertEquals(SolicitudCertificacion.Estado.PENDIENTE, s.estado());
        assertEquals(BLOQUE, s.energiaWh());
        // tras reservar el bloque, quedan 100_000 Wh y 1 bloque reservado
        BalanceGeneracionCertificable b = service.balanceDe("MED-1");
        assertEquals(100_000, b.generacionAcumuladaWh());
        assertEquals(1, b.bloquesReservados());
    }

    @Test
    @DisplayName("no se puede solicitar sin generación suficiente para un bloque")
    void noSolicitaSinGeneracionSuficiente() {
        service.registrarGeneracion(new ComandoRegistrarGeneracion("MED-1", "m1", 500_000));
        assertThrows(IllegalStateException.class,
                () -> service.solicitarCertificacion(new ComandoSolicitar("MED-1", "0xPROP", 202610)));
    }

    @Test
    @DisplayName("requiere los DOS avales (Voltera + certificadora) para quedar APROBADA")
    void requiereDobleAval() {
        service.registrarGeneracion(new ComandoRegistrarGeneracion("MED-1", "m1", 1_000_000));
        SolicitudCertificacion s = service.solicitarCertificacion(new ComandoSolicitar("MED-1", "0xPROP", 202610));

        service.avalarVoltera(s.id());
        assertEquals(SolicitudCertificacion.Estado.PENDIENTE, service.porId(s.id()).estado());
        service.avalarCertificadora(s.id());
        assertEquals(SolicitudCertificacion.Estado.APROBADA, service.porId(s.id()).estado());
        assertTrue(service.porId(s.id()).estaAprobada());
    }

    @Test
    @DisplayName("una solicitud rechazada no admite más avales")
    void rechazadaNoAdmiteAvales() {
        service.registrarGeneracion(new ComandoRegistrarGeneracion("MED-1", "m1", 1_000_000));
        SolicitudCertificacion s = service.solicitarCertificacion(new ComandoSolicitar("MED-1", "0xPROP", 202610));
        service.rechazar(s.id(), "medición sospechosa");
        assertEquals(SolicitudCertificacion.Estado.RECHAZADA, service.porId(s.id()).estado());
        assertThrows(IllegalStateException.class, () -> service.avalarVoltera(s.id()));
    }
}
