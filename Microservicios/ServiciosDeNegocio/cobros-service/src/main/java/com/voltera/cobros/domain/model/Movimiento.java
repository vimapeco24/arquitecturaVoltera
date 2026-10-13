package com.voltera.cobros.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Agregado raíz del BC <b>Cobros</b> (lámina 02 · "Contexto Cobros → agregado
 * Movimiento (raíz), con Prima y Recibo").
 *
 * <p>Un Movimiento modela el cobro de una prima a un cliente. Nace
 * {@link EstadoMovimiento#PENDIENTE} con su {@link Prima}. Cuando llega el evento
 * {@code PagoConfirmado} (emitido por el contexto de Pagos), el movimiento se
 * <b>concilia</b>: emite un {@link Recibo} y pasa a {@link EstadoMovimiento#CONCILIADO}.
 * La conciliación es idempotente por la referencia del pago.</p>
 */
public class Movimiento {

    private final MovimientoId id;
    private final String clienteId;
    private final String polizaId;
    private final Prima prima;
    private final Instant registradoEn;

    private EstadoMovimiento estado;
    private Recibo recibo;
    private Instant conciliadoEn;

    private Movimiento(MovimientoId id, String clienteId, String polizaId, Prima prima, Instant registradoEn) {
        this.id = id;
        this.clienteId = clienteId;
        this.polizaId = polizaId;
        this.prima = prima;
        this.registradoEn = registradoEn;
        this.estado = EstadoMovimiento.PENDIENTE;
    }

    /** Registra un nuevo movimiento de cobro para una prima (estado PENDIENTE). */
    public static Movimiento registrar(String clienteId, String polizaId, Prima prima) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("clienteId es obligatorio");
        }
        if (polizaId == null || polizaId.isBlank()) {
            throw new IllegalArgumentException("polizaId es obligatorio");
        }
        if (prima == null) {
            throw new IllegalArgumentException("La prima es obligatoria");
        }
        return new Movimiento(MovimientoId.nuevo(), clienteId, polizaId, prima, Instant.now());
    }

    /** Rehidratación desde persistencia. */
    public static Movimiento rehidratar(MovimientoId id, String clienteId, String polizaId, Prima prima,
                                        Instant registradoEn, EstadoMovimiento estado,
                                        Recibo recibo, Instant conciliadoEn) {
        Movimiento m = new Movimiento(id, clienteId, polizaId, prima, registradoEn);
        m.estado = estado;
        m.recibo = recibo;
        m.conciliadoEn = conciliadoEn;
        return m;
    }

    /**
     * Concilia el movimiento con un pago confirmado: emite el {@link Recibo} y
     * pasa a CONCILIADO. Idempotente: si ya está conciliado, no vuelve a emitir.
     *
     * @return true si la conciliación se aplicó ahora; false si ya estaba conciliado.
     */
    public boolean conciliarConPago(String referenciaPago, BigDecimal montoPagado) {
        if (estado == EstadoMovimiento.CONCILIADO) {
            return false;
        }
        this.recibo = Recibo.emitir(referenciaPago, montoPagado);
        this.estado = EstadoMovimiento.CONCILIADO;
        this.conciliadoEn = Instant.now();
        return true;
    }

    public MovimientoId id() { return id; }
    public String clienteId() { return clienteId; }
    public String polizaId() { return polizaId; }
    public Prima prima() { return prima; }
    public Instant registradoEn() { return registradoEn; }
    public EstadoMovimiento estado() { return estado; }
    public Recibo recibo() { return recibo; }
    public Instant conciliadoEn() { return conciliadoEn; }
}
