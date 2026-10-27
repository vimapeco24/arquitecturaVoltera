package com.voltera.blockchain.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Certificado de Origen Renovable tokenizado como NFT (ERC-721 NO fungible).
 * Refleja el contrato {@code CertificadoOrigenNFT.sol} (lamina 05/06).
 *
 * <p>Atributos on-chain: idMedidor, propietario (owner ERC-721), emisor,
 * consumoNetoMensualWh y periodo. La metadata (tokenURI) y el documento viven
 * OFF-CHAIN; on-chain va el hash de integridad. El historial recoge la cadena de
 * custodia billetera -> billetera (emision, transferencias, quema).</p>
 */
public class CertificadoNft {

    public enum TipoMovimiento { EMISION, TRANSFERENCIA, QUEMA }

    /** Un movimiento de propiedad: de que billetera a que billetera. */
    public record Movimiento(
            TipoMovimiento tipo,
            String desde,
            String hacia,
            String ejecutadoPor,
            Instant ts,
            String txHash,
            String nota) {}

    public static final String ZERO = "0x0000000000000000000000000000000000000000";

    private final long tokenId;
    private final String idMedidor;
    private String propietario;
    private final String emisor;
    private final long consumoNetoMensualWh;
    private final int periodo;
    private final String tokenUri;
    private final String hashDetalle;
    private final Instant emitidoEn;
    private boolean quemado;
    private final List<Movimiento> historial = new ArrayList<>();

    public CertificadoNft(long tokenId, String idMedidor, String propietario, String emisor,
                          long consumoNetoMensualWh, int periodo, String tokenUri,
                          String hashDetalle, Instant emitidoEn) {
        this.tokenId = tokenId;
        this.idMedidor = idMedidor;
        this.propietario = propietario;
        this.emisor = emisor;
        this.consumoNetoMensualWh = consumoNetoMensualWh;
        this.periodo = periodo;
        this.tokenUri = tokenUri;
        this.hashDetalle = hashDetalle;
        this.emitidoEn = emitidoEn;
    }

    /** Registra la acuñacion (mint): nace desde la direccion cero hacia el propietario. */
    public void registrarEmision(String txHash) {
        historial.add(new Movimiento(TipoMovimiento.EMISION, ZERO, propietario, emisor,
                emitidoEn, txHash, "Acuñación (mint) del certificado de origen"));
    }

    /** Transfiere la propiedad de una billetera a otra y lo deja en el historial. */
    public void transferir(String hacia, String txHash) {
        String desde = this.propietario;
        this.propietario = hacia;
        historial.add(new Movimiento(TipoMovimiento.TRANSFERENCIA, desde, hacia, desde,
                Instant.now(), txHash, "Transferencia de propiedad del certificado"));
    }

    /** Quema (retira) el certificado: va del propietario a la direccion cero. */
    public void quemar(String txHash) {
        this.quemado = true;
        historial.add(new Movimiento(TipoMovimiento.QUEMA, this.propietario, ZERO, this.propietario,
                Instant.now(), txHash, "Retiro (burn) del certificado"));
    }

    public long tokenId() { return tokenId; }
    public String idMedidor() { return idMedidor; }
    public String propietario() { return propietario; }
    public String emisor() { return emisor; }
    public long consumoNetoMensualWh() { return consumoNetoMensualWh; }
    public int periodo() { return periodo; }
    public String tokenUri() { return tokenUri; }
    public String hashDetalle() { return hashDetalle; }
    public Instant emitidoEn() { return emitidoEn; }
    public boolean quemado() { return quemado; }
    public List<Movimiento> historial() { return Collections.unmodifiableList(historial); }

    /** Clave de unicidad (medidor, periodo) para rechazar duplicados. */
    public String claveUnicidad() { return claveUnicidad(idMedidor, periodo); }

    public static String claveUnicidad(String idMedidor, int periodo) {
        return idMedidor + "|" + periodo;
    }
}
