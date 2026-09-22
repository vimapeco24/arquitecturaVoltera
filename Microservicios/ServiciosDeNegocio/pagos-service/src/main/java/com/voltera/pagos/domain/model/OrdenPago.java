package com.voltera.pagos.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad Raiz («root») del agregado OrdenPago.
 * Modela un cobro o pago con maquina de estados e idempotencia por
 * referencia externa (id de factura o transaccion).
 */
public class OrdenPago {

    private final PagoId id;
    private final String prosumidorId;
    private final String referencia;   // id de factura/transaccion (clave de idempotencia)
    private final TipoPago tipo;
    private final Monto monto;
    private EstadoPago estado;
    private String motivoFallo;
    private final Instant creadaEn;

    private OrdenPago(PagoId id, String prosumidorId, String referencia, TipoPago tipo,
                      Monto monto, EstadoPago estado, Instant creadaEn) {
        this.id = id;
        this.prosumidorId = prosumidorId;
        this.referencia = referencia;
        this.tipo = tipo;
        this.monto = monto;
        this.estado = estado;
        this.creadaEn = creadaEn;
    }

    public static OrdenPago crear(String prosumidorId, String referencia, TipoPago tipo, Monto monto) {
        if (prosumidorId == null || prosumidorId.isBlank()) {
            throw new IllegalArgumentException("prosumidorId requerido");
        }
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("referencia requerida (clave de idempotencia)");
        }
        Objects.requireNonNull(tipo, "tipo requerido");
        Objects.requireNonNull(monto, "monto requerido");
        return new OrdenPago(PagoId.nuevo(), prosumidorId, referencia, tipo, monto,
                EstadoPago.PENDIENTE, Instant.now());
    }

    /**
     * Factory de dominio: crea una orden CONSERVANDO el id dado, aplicando las
     * mismas invariantes que {@link #crear}. La orden nace PENDIENTE.
     */
    public static OrdenPago crearCon(PagoId id, String prosumidorId, String referencia,
                                     TipoPago tipo, Monto monto) {
        Objects.requireNonNull(id, "id requerido");
        if (prosumidorId == null || prosumidorId.isBlank()) {
            throw new IllegalArgumentException("prosumidorId requerido");
        }
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("referencia requerida (clave de idempotencia)");
        }
        Objects.requireNonNull(tipo, "tipo requerido");
        Objects.requireNonNull(monto, "monto requerido");
        return new OrdenPago(id, prosumidorId, referencia, tipo, monto,
                EstadoPago.PENDIENTE, Instant.now());
    }

    public static OrdenPago reconstituir(PagoId id, String prosumidorId, String referencia,
                                         TipoPago tipo, Monto monto, EstadoPago estado, Instant creadaEn) {
        return new OrdenPago(id, prosumidorId, referencia, tipo, monto, estado, creadaEn);
    }

    /** Invariante: solo una orden PENDIENTE puede procesarse. */
    public void marcarProcesada() {
        if (estado != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo una orden PENDIENTE puede procesarse");
        }
        this.estado = EstadoPago.PROCESADO;
    }

    /** Invariante: solo una orden PENDIENTE puede fallar. */
    public void marcarFallida(String motivo) {
        if (estado != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo una orden PENDIENTE puede marcarse como FALLIDA");
        }
        this.estado = EstadoPago.FALLIDO;
        this.motivoFallo = motivo;
    }

    public PagoId id() { return id; }
    public String prosumidorId() { return prosumidorId; }
    public String referencia() { return referencia; }
    public TipoPago tipo() { return tipo; }
    public Monto monto() { return monto; }
    public EstadoPago estado() { return estado; }
    public String motivoFallo() { return motivoFallo; }
    public Instant creadaEn() { return creadaEn; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrdenPago op)) return false;
        return id.equals(op.id);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
