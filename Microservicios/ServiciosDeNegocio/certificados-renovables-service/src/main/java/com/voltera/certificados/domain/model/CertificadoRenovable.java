package com.voltera.certificados.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agregado raíz (lámina 04): el certificado de origen renovable desde la óptica
 * del DOMINIO (no de la cadena). Gestiona emisión, titularidad, transferencia y
 * retiro. El reflejo on-chain (NFT ERC-721) lo produce el gateway; este agregado
 * guarda el ESTADO LOCAL y se actualiza tras la confirmación del gateway.
 *
 * <p>Estado de confirmación (patrón store-and-forward):
 * SOLICITADO → (gateway confirma) → CONFIRMADO; o FALLIDO.
 * Estado de dominio: EMITIDO → (transferencias) → RETIRADO.</p>
 */
public class CertificadoRenovable {

    public enum EstadoDominio { EMITIDO, RETIRADO }
    public enum EstadoCadena { SOLICITADO, CONFIRMADO, FALLIDO }

    public record Movimiento(String tipo, String desde, String hacia, Instant ts, String txHash) {}

    public static final String ZERO = "0x0000000000000000000000000000000000000000";

    private final String id;                 // id de dominio (p. ej. CERT-1)
    private final String idMedidor;
    private final String emisor;
    private String titular;                  // propietario actual
    private final long consumoNetoMensualWh;
    private final int periodo;               // AAAAMM
    private final String solicitudCertificacionId; // trazabilidad al BC de certificación

    private EstadoDominio estadoDominio = EstadoDominio.EMITIDO;
    private EstadoCadena estadoCadena = EstadoCadena.SOLICITADO;

    private Long tokenId;                     // lo asigna el gateway al confirmar
    private String txHash;
    private String ultimoError;
    private final Instant creadoEn = Instant.now();
    private final List<Movimiento> historial = new ArrayList<>();

    public CertificadoRenovable(String id, String idMedidor, String emisor, String titular,
                                long consumoNetoMensualWh, int periodo, String solicitudCertificacionId) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id requerido");
        if (idMedidor == null || idMedidor.isBlank()) throw new IllegalArgumentException("idMedidor requerido");
        if (emisor == null || emisor.isBlank()) throw new IllegalArgumentException("emisor requerido");
        if (titular == null || titular.isBlank()) throw new IllegalArgumentException("titular requerido");
        if (consumoNetoMensualWh <= 0) throw new IllegalArgumentException("consumoNetoMensualWh debe ser > 0");
        if (periodo <= 0) throw new IllegalArgumentException("periodo (AAAAMM) debe ser > 0");
        this.id = id;
        this.idMedidor = idMedidor;
        this.emisor = emisor;
        this.titular = titular;
        this.consumoNetoMensualWh = consumoNetoMensualWh;
        this.periodo = periodo;
        this.solicitudCertificacionId = solicitudCertificacionId;
        historial.add(new Movimiento("EMISION", ZERO, titular, creadoEn, null));
    }

    /** El gateway confirmó la emisión del NFT: guardamos tokenId y txHash. */
    public void confirmarEmision(long tokenId, String txHash) {
        this.tokenId = tokenId;
        this.txHash = txHash;
        this.estadoCadena = EstadoCadena.CONFIRMADO;
        if (!historial.isEmpty()) {
            Movimiento m = historial.get(0);
            historial.set(0, new Movimiento(m.tipo(), m.desde(), m.hacia(), m.ts(), txHash));
        }
    }

    public void marcarFallido(String error) {
        this.estadoCadena = EstadoCadena.FALLIDO;
        this.ultimoError = error;
    }

    /** Transfiere la titularidad de una billetera a otra. */
    public void transferir(String hacia, String txHash) {
        exigirActivo();
        if (hacia == null || hacia.isBlank() || hacia.equals(ZERO)) {
            throw new IllegalArgumentException("billetera destino inválida");
        }
        if (hacia.equalsIgnoreCase(titular)) {
            throw new IllegalArgumentException("el destino ya es el titular actual");
        }
        String desde = this.titular;
        this.titular = hacia;
        historial.add(new Movimiento("TRANSFERENCIA", desde, hacia, Instant.now(), txHash));
    }

    /** Retira (quema) el certificado. */
    public void retirar(String txHash) {
        exigirActivo();
        this.estadoDominio = EstadoDominio.RETIRADO;
        historial.add(new Movimiento("RETIRO", titular, ZERO, Instant.now(), txHash));
    }

    private void exigirActivo() {
        if (estadoDominio == EstadoDominio.RETIRADO) {
            throw new IllegalStateException("El certificado " + id + " está retirado");
        }
    }

    public String id() { return id; }
    public String idMedidor() { return idMedidor; }
    public String emisor() { return emisor; }
    public String titular() { return titular; }
    public long consumoNetoMensualWh() { return consumoNetoMensualWh; }
    public int periodo() { return periodo; }
    public String solicitudCertificacionId() { return solicitudCertificacionId; }
    public EstadoDominio estadoDominio() { return estadoDominio; }
    public EstadoCadena estadoCadena() { return estadoCadena; }
    public Long tokenId() { return tokenId; }
    public String txHash() { return txHash; }
    public String ultimoError() { return ultimoError; }
    public Instant creadoEn() { return creadoEn; }
    public List<Movimiento> historial() { return Collections.unmodifiableList(historial); }
}
