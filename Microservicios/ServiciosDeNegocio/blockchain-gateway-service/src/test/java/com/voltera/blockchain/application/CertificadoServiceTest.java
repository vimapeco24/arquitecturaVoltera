package com.voltera.blockchain.application;

import com.voltera.blockchain.domain.model.CertificadoNft;
import com.voltera.blockchain.domain.port.in.GestionarCertificadoUseCase.ComandoEmitir;
import com.voltera.blockchain.infrastructure.blockchain.CertificadoNftDemoAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del ciclo de vida de los certificados NFT con el adaptador DEMO
 * (sin cadena real): emision, unicidad, transferencia billetera->billetera y quema.
 */
class CertificadoServiceTest {

    private static final String EMISOR = "0xA0A18C1D64272f9a6a248Deb5106994F5D8AFDf2";
    private static final String PROP = "0x1111111111111111111111111111111111111111";
    private static final String OTRO = "0x2222222222222222222222222222222222222222";

    private CertificadoService service;

    @BeforeEach
    void setUp() {
        service = new CertificadoService(new CertificadoNftDemoAdapter());
    }

    private ComandoEmitir cmd(String medidor, int periodo) {
        return new ComandoEmitir(EMISOR, PROP, medidor, 120_000L, periodo);
    }

    @Test
    void emite_un_certificado_con_sus_atributos_y_registra_la_emision() {
        CertificadoNft c = service.emitir(cmd("MEDIDOR-IOT-0001", 202610));

        assertThat(c.tokenId()).isEqualTo(1L);
        assertThat(c.idMedidor()).isEqualTo("MEDIDOR-IOT-0001");
        assertThat(c.propietario()).isEqualTo(PROP);
        assertThat(c.emisor()).isEqualTo(EMISOR);
        assertThat(c.consumoNetoMensualWh()).isEqualTo(120_000L);
        assertThat(c.periodo()).isEqualTo(202610);
        assertThat(c.hashDetalle()).startsWith("0x").hasSize(66);
        assertThat(c.historial()).hasSize(1);
        assertThat(c.historial().get(0).tipo()).isEqualTo(CertificadoNft.TipoMovimiento.EMISION);
        assertThat(c.historial().get(0).desde()).isEqualTo(CertificadoNft.ZERO);
        assertThat(c.historial().get(0).hacia()).isEqualTo(PROP);
    }

    @Test
    void rechaza_emision_duplicada_para_el_mismo_medidor_y_periodo() {
        service.emitir(cmd("MEDIDOR-IOT-0001", 202610));
        assertThatThrownBy(() -> service.emitir(cmd("MEDIDOR-IOT-0001", 202610)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicada");
        // el mismo medidor en otro periodo si se permite
        CertificadoNft otro = service.emitir(cmd("MEDIDOR-IOT-0001", 202611));
        assertThat(otro.tokenId()).isEqualTo(2L);
    }

    @Test
    void transfiere_de_una_billetera_a_otra_y_lo_deja_en_el_historial() {
        CertificadoNft c = service.emitir(cmd("MEDIDOR-IOT-0001", 202610));
        CertificadoNft t = service.transferir(c.tokenId(), OTRO);

        assertThat(t.propietario()).isEqualTo(OTRO);
        assertThat(t.historial()).hasSize(2);
        var mov = t.historial().get(1);
        assertThat(mov.tipo()).isEqualTo(CertificadoNft.TipoMovimiento.TRANSFERENCIA);
        assertThat(mov.desde()).isEqualTo(PROP);
        assertThat(mov.hacia()).isEqualTo(OTRO);
    }

    @Test
    void no_transfiere_al_mismo_propietario_ni_a_direccion_cero() {
        CertificadoNft c = service.emitir(cmd("MEDIDOR-IOT-0001", 202610));
        assertThatThrownBy(() -> service.transferir(c.tokenId(), PROP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.transferir(c.tokenId(), CertificadoNft.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void quema_el_certificado_y_no_permite_transferirlo_luego() {
        CertificadoNft c = service.emitir(cmd("MEDIDOR-IOT-0001", 202610));
        CertificadoNft q = service.quemar(c.tokenId());
        assertThat(q.quemado()).isTrue();
        assertThat(q.historial().get(q.historial().size() - 1).tipo())
                .isEqualTo(CertificadoNft.TipoMovimiento.QUEMA);
        assertThatThrownBy(() -> service.transferir(c.tokenId(), OTRO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void filtra_por_propietario_actual() {
        CertificadoNft c1 = service.emitir(cmd("MEDIDOR-IOT-0001", 202610));
        service.emitir(cmd("MEDIDOR-IOT-0002", 202610));
        service.transferir(c1.tokenId(), OTRO);

        assertThat(service.porPropietario(OTRO)).extracting(CertificadoNft::tokenId).containsExactly(1L);
        assertThat(service.porPropietario(PROP)).extracting(CertificadoNft::tokenId).containsExactly(2L);
        assertThat(service.todos()).hasSize(2);
    }

    @Test
    void valida_datos_de_entrada() {
        assertThatThrownBy(() -> service.emitir(new ComandoEmitir(EMISOR, PROP, "", 10L, 202610)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.emitir(new ComandoEmitir(EMISOR, PROP, "M", 0L, 202610)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.emitir(new ComandoEmitir(EMISOR, PROP, "M", 10L, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
