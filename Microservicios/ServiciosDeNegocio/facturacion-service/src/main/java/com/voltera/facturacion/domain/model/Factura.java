package com.voltera.facturacion.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad Raiz («root») del agregado Factura.
 * Unico punto de entrada: nadie modifica el estado interno sin pasar por ella.
 * Protege las invariantes del dominio de facturacion.
 */
public class Factura {

    private final FacturaId id;
    private final String prosumidorId;
    private final PeriodoFacturacion periodo;
    private final ConsumoNeto consumoNeto;
    private final Tarifa tarifa;
    private final Dinero cargoConsumo;
    private final Dinero creditoExcedente;
    private final Dinero total;
    private EstadoFactura estado;
    private final Instant emitidaEn;

    private Factura(FacturaId id, String prosumidorId, PeriodoFacturacion periodo,
                    ConsumoNeto consumoNeto, Tarifa tarifa, Dinero cargoConsumo,
                    Dinero creditoExcedente, Dinero total, EstadoFactura estado, Instant emitidaEn) {
        this.id = id;
        this.prosumidorId = prosumidorId;
        this.periodo = periodo;
        this.consumoNeto = consumoNeto;
        this.tarifa = tarifa;
        this.cargoConsumo = cargoConsumo;
        this.creditoExcedente = creditoExcedente;
        this.total = total;
        this.estado = estado;
        this.emitidaEn = emitidaEn;
    }

    /**
     * Factory de dominio: emite una factura calculando cargos y creditos
     * a partir del consumo neto y la tarifa. Aqui vive la logica de negocio.
     */
    public static Factura emitir(FacturaId id, String prosumidorId, PeriodoFacturacion periodo,
                                 ConsumoNeto consumoNeto, Tarifa tarifa) {
        Objects.requireNonNull(id, "id requerido");
        if (prosumidorId == null || prosumidorId.isBlank()) {
            throw new IllegalArgumentException("prosumidorId requerido");
        }
        Objects.requireNonNull(periodo, "periodo requerido");
        Objects.requireNonNull(consumoNeto, "consumoNeto requerido");
        Objects.requireNonNull(tarifa, "tarifa requerido");

        Dinero cargo = tarifa.valorarConsumo(consumoNeto.consumoFacturable());
        Dinero credito = tarifa.valorarExcedente(consumoNeto.excedente());
        Dinero total = cargo.restar(credito);

        return new Factura(id, prosumidorId, periodo, consumoNeto, tarifa,
                cargo, credito, total, EstadoFactura.EMITIDA, Instant.now());
    }

    /** Reconstruccion desde persistencia (adaptador de salida). */
    public static Factura reconstituir(FacturaId id, String prosumidorId, PeriodoFacturacion periodo,
                                       ConsumoNeto consumoNeto, Tarifa tarifa, Dinero cargoConsumo,
                                       Dinero creditoExcedente, Dinero total, EstadoFactura estado,
                                       Instant emitidaEn) {
        return new Factura(id, prosumidorId, periodo, consumoNeto, tarifa,
                cargoConsumo, creditoExcedente, total, estado, emitidaEn);
    }

    /** Invariante: solo se puede pagar una factura emitida. */
    public void marcarPagada() {
        if (estado != EstadoFactura.EMITIDA) {
            throw new IllegalStateException("Solo una factura EMITIDA puede marcarse como PAGADA");
        }
        this.estado = EstadoFactura.PAGADA;
    }

    /** Invariante: no se puede anular una factura ya pagada. */
    public void anular() {
        if (estado == EstadoFactura.PAGADA) {
            throw new IllegalStateException("No se puede anular una factura ya PAGADA");
        }
        this.estado = EstadoFactura.ANULADA;
    }

    /** True si el prosumidor tiene saldo a favor (genero mas de lo que consumio). */
    public boolean tieneSaldoAFavor() {
        return total.esNegativo();
    }

    public FacturaId id() { return id; }
    public String prosumidorId() { return prosumidorId; }
    public PeriodoFacturacion periodo() { return periodo; }
    public ConsumoNeto consumoNeto() { return consumoNeto; }
    public Tarifa tarifa() { return tarifa; }
    public Dinero cargoConsumo() { return cargoConsumo; }
    public Dinero creditoExcedente() { return creditoExcedente; }
    public Dinero total() { return total; }
    public EstadoFactura estado() { return estado; }
    public Instant emitidaEn() { return emitidaEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Factura f)) return false;
        return id.equals(f.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
