package com.voltera.blockchain.domain.model;

import java.time.Instant;

/**
 * Entidad raiz del agregado OUTBOX de anclaje. Representa la intencion de anclar
 * una liquidacion P2P en la cadena y su progreso. Es la frontera de consistencia
 * entre el dominio y la cadena: primero se persiste PENDIENTE (store-and-forward),
 * luego el relay la lleva a CONFIRMADO o FALLIDO.
 *
 * <p>El {@code comunidadId} es un metadato OFF-CHAIN de agrupacion (a que comunidad
 * energetica pertenece la casacion). NO entra en la representacion canonica ni en
 * el hash anclado, para no romper la idempotencia/verificacion del contrato ya
 * desplegado: la cadena solo conoce el hash (gate 8). {@code bloque} y {@code gasUsado}
 * se rellenan desde el recibo on-chain cuando la transaccion queda minada.</p>
 */
public class Anclaje {

    private final String transaccionId;
    private final String hashCasacion;   // keccak256 (0x...) del registro canonico
    private final long energiaWh;
    private final long liquidadaEnEpoch;
    private final String comunidadId;    // metadato off-chain (puede ser null)
    private EstadoAnclaje estado;
    private int intentos;
    private String txHash;               // hash de la transaccion on-chain (cuando confirma)
    private Long bloque;                 // numero de bloque del recibo (cuando confirma)
    private Long gasUsado;               // gas usado segun el recibo (cuando confirma)
    private String ultimoError;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    private Anclaje(String transaccionId, String hashCasacion, long energiaWh,
                    long liquidadaEnEpoch, String comunidadId, EstadoAnclaje estado, int intentos,
                    String txHash, Long bloque, Long gasUsado, String ultimoError,
                    Instant creadoEn, Instant actualizadoEn) {
        this.transaccionId = transaccionId;
        this.hashCasacion = hashCasacion;
        this.energiaWh = energiaWh;
        this.liquidadaEnEpoch = liquidadaEnEpoch;
        this.comunidadId = comunidadId;
        this.estado = estado;
        this.intentos = intentos;
        this.txHash = txHash;
        this.bloque = bloque;
        this.gasUsado = gasUsado;
        this.ultimoError = ultimoError;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    /** Factory: crea un anclaje nuevo en estado PENDIENTE (sin comunidad). */
    public static Anclaje pendiente(String transaccionId, String hashCasacion,
                                    long energiaWh, long liquidadaEnEpoch) {
        return pendiente(transaccionId, hashCasacion, energiaWh, liquidadaEnEpoch, null);
    }

    /** Factory: crea un anclaje nuevo en estado PENDIENTE asociado a una comunidad. */
    public static Anclaje pendiente(String transaccionId, String hashCasacion,
                                    long energiaWh, long liquidadaEnEpoch, String comunidadId) {
        Instant ahora = Instant.now();
        return new Anclaje(transaccionId, hashCasacion, energiaWh, liquidadaEnEpoch,
                normalizar(comunidadId), EstadoAnclaje.PENDIENTE, 0, null, null, null, null, ahora, ahora);
    }

    /** Reconstituye desde persistencia. */
    public static Anclaje reconstituir(String transaccionId, String hashCasacion, long energiaWh,
                                       long liquidadaEnEpoch, String comunidadId, EstadoAnclaje estado,
                                       int intentos, String txHash, Long bloque, Long gasUsado,
                                       String ultimoError, Instant creadoEn, Instant actualizadoEn) {
        return new Anclaje(transaccionId, hashCasacion, energiaWh, liquidadaEnEpoch,
                normalizar(comunidadId), estado, intentos, txHash, bloque, gasUsado,
                ultimoError, creadoEn, actualizadoEn);
    }

    private static String normalizar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Confirma el anclaje con solo el txHash (sin datos de recibo). */
    public void marcarConfirmado(String txHash) {
        marcarConfirmado(txHash, null, null);
    }

    /** Confirma el anclaje con el txHash y los datos del recibo on-chain. */
    public void marcarConfirmado(String txHash, Long bloque, Long gasUsado) {
        this.estado = EstadoAnclaje.CONFIRMADO;
        this.txHash = txHash;
        this.bloque = bloque;
        this.gasUsado = gasUsado;
        this.ultimoError = null;
        this.actualizadoEn = Instant.now();
    }

    public void registrarIntentoFallido(String error, boolean definitivo) {
        this.intentos++;
        this.ultimoError = error;
        this.estado = definitivo ? EstadoAnclaje.FALLIDO : EstadoAnclaje.PENDIENTE;
        this.actualizadoEn = Instant.now();
    }

    public boolean estaPendiente() { return estado == EstadoAnclaje.PENDIENTE; }
    public boolean estaConfirmado() { return estado == EstadoAnclaje.CONFIRMADO; }

    public String transaccionId() { return transaccionId; }
    public String hashCasacion() { return hashCasacion; }
    public long energiaWh() { return energiaWh; }
    public long liquidadaEnEpoch() { return liquidadaEnEpoch; }
    public String comunidadId() { return comunidadId; }
    public EstadoAnclaje estado() { return estado; }
    public int intentos() { return intentos; }
    public String txHash() { return txHash; }
    public Long bloque() { return bloque; }
    public Long gasUsado() { return gasUsado; }
    public String ultimoError() { return ultimoError; }
    public Instant creadoEn() { return creadoEn; }
    public Instant actualizadoEn() { return actualizadoEn; }
}
