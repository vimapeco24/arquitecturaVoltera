package com.voltera.blockchain.domain.model;

import java.time.Instant;

/**
 * Entidad raiz del agregado OUTBOX de anclaje. Representa la intencion de anclar
 * una liquidacion P2P en la cadena y su progreso. Es la frontera de consistencia
 * entre el dominio y la cadena: primero se persiste PENDIENTE (store-and-forward),
 * luego el relay la lleva a CONFIRMADO o FALLIDO.
 */
public class Anclaje {

    private final String transaccionId;
    private final String hashCasacion;   // keccak256 (0x...) del registro canonico
    private final long energiaWh;
    private final long liquidadaEnEpoch;
    private EstadoAnclaje estado;
    private int intentos;
    private String txHash;               // hash de la transaccion on-chain (cuando confirma)
    private String ultimoError;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    private Anclaje(String transaccionId, String hashCasacion, long energiaWh,
                    long liquidadaEnEpoch, EstadoAnclaje estado, int intentos,
                    String txHash, String ultimoError, Instant creadoEn, Instant actualizadoEn) {
        this.transaccionId = transaccionId;
        this.hashCasacion = hashCasacion;
        this.energiaWh = energiaWh;
        this.liquidadaEnEpoch = liquidadaEnEpoch;
        this.estado = estado;
        this.intentos = intentos;
        this.txHash = txHash;
        this.ultimoError = ultimoError;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    /** Factory: crea un anclaje nuevo en estado PENDIENTE. */
    public static Anclaje pendiente(String transaccionId, String hashCasacion,
                                    long energiaWh, long liquidadaEnEpoch) {
        Instant ahora = Instant.now();
        return new Anclaje(transaccionId, hashCasacion, energiaWh, liquidadaEnEpoch,
                EstadoAnclaje.PENDIENTE, 0, null, null, ahora, ahora);
    }

    /** Reconstituye desde persistencia. */
    public static Anclaje reconstituir(String transaccionId, String hashCasacion, long energiaWh,
                                       long liquidadaEnEpoch, EstadoAnclaje estado, int intentos,
                                       String txHash, String ultimoError,
                                       Instant creadoEn, Instant actualizadoEn) {
        return new Anclaje(transaccionId, hashCasacion, energiaWh, liquidadaEnEpoch,
                estado, intentos, txHash, ultimoError, creadoEn, actualizadoEn);
    }

    public void marcarConfirmado(String txHash) {
        this.estado = EstadoAnclaje.CONFIRMADO;
        this.txHash = txHash;
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
    public EstadoAnclaje estado() { return estado; }
    public int intentos() { return intentos; }
    public String txHash() { return txHash; }
    public String ultimoError() { return ultimoError; }
    public Instant creadoEn() { return creadoEn; }
    public Instant actualizadoEn() { return actualizadoEn; }
}
