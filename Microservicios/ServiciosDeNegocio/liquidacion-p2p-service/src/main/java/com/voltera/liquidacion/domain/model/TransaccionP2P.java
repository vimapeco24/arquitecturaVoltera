package com.voltera.liquidacion.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad Raiz («root») del agregado TransaccionP2P.
 * Representa un intercambio efectivo de energia entre dos prosumidores,
 * resultado de casar una orden de venta con una de compra.
 * Es la frontera de consistencia: precio, cantidad y partes se fijan juntos.
 */
public class TransaccionP2P {

    private final TransaccionId id;
    private final OrdenId ordenVentaId;
    private final OrdenId ordenCompraId;
    private final String vendedorId;
    private final String compradorId;
    private final Energia energia;
    private final Precio precioCasacion;
    private final BigDecimal valorTotal;
    private final Instant liquidadaEn;
    // Resultado de la casacion (informacion de negocio para el consumidor):
    private final Energia energiaSolicitadaVenta;
    private final Energia energiaSolicitadaCompra;
    private final Energia pendienteVenta;
    private final Energia pendienteCompra;

    private TransaccionP2P(TransaccionId id, OrdenId ordenVentaId, OrdenId ordenCompraId,
                           String vendedorId, String compradorId, Energia energia,
                           Precio precioCasacion, BigDecimal valorTotal, Instant liquidadaEn,
                           Energia energiaSolicitadaVenta, Energia energiaSolicitadaCompra,
                           Energia pendienteVenta, Energia pendienteCompra) {
        this.id = id;
        this.ordenVentaId = ordenVentaId;
        this.ordenCompraId = ordenCompraId;
        this.vendedorId = vendedorId;
        this.compradorId = compradorId;
        this.energia = energia;
        this.precioCasacion = precioCasacion;
        this.valorTotal = valorTotal;
        this.liquidadaEn = liquidadaEn;
        this.energiaSolicitadaVenta = energiaSolicitadaVenta;
        this.energiaSolicitadaCompra = energiaSolicitadaCompra;
        this.pendienteVenta = pendienteVenta;
        this.pendienteCompra = pendienteCompra;
    }

    /**
     * Factory de dominio: liquida una transaccion a partir de dos ordenes casadas.
     * Invariantes:
     *  - venta y compra deben ser de tipos opuestos
     *  - un prosumidor no puede comerciar consigo mismo
     *  - el precio de compra debe cubrir el de venta (compra >= venta)
     *  - la energia casada es el minimo pendiente de ambas ordenes
     */
    public static TransaccionP2P liquidar(TransaccionId id, OrdenMercado venta, OrdenMercado compra) {
        Objects.requireNonNull(id, "id requerido");
        Objects.requireNonNull(venta, "orden de venta requerida");
        Objects.requireNonNull(compra, "orden de compra requerida");

        if (venta.tipo() != TipoOrden.VENTA) {
            throw new IllegalArgumentException("La primera orden debe ser de tipo VENTA");
        }
        if (compra.tipo() != TipoOrden.COMPRA) {
            throw new IllegalArgumentException("La segunda orden debe ser de tipo COMPRA");
        }
        if (venta.prosumidorId().equals(compra.prosumidorId())) {
            throw new IllegalArgumentException("Un prosumidor no puede comerciar consigo mismo");
        }
        // Compatibilidad de precio: el comprador acepta pagar al menos el limite del vendedor.
        if (!venta.precioLimite().menorOIgualQue(compra.precioLimite())) {
            throw new IllegalArgumentException(
                    "No hay casacion: el precio de venta excede el de compra");
        }

        Energia casada = venta.pendiente().minimo(compra.pendiente());
        if (casada.esCero()) {
            throw new IllegalStateException("No hay energia pendiente para casar");
        }

        // Precio de casacion: promedio entre ambos limites (mercado justo).
        Precio precio = venta.precioLimite().promedioCon(compra.precioLimite());
        BigDecimal total = precio.valorar(casada);

        // Energia solicitada por cada parte ANTES de casar (para reportar el match).
        Energia solicitadaVenta = venta.pendiente();
        Energia solicitadaCompra = compra.pendiente();

        // Efecto sobre las ordenes (mutacion controlada por el dominio).
        venta.reducirPendiente(casada);
        compra.reducirPendiente(casada);

        return new TransaccionP2P(id, venta.id(), compra.id(),
                venta.prosumidorId(), compra.prosumidorId(),
                casada, precio, total, Instant.now(),
                solicitadaVenta, solicitadaCompra, venta.pendiente(), compra.pendiente());
    }

    public static TransaccionP2P reconstituir(TransaccionId id, OrdenId ventaId, OrdenId compraId,
                                              String vendedorId, String compradorId, Energia energia,
                                              Precio precio, BigDecimal valorTotal, Instant liquidadaEn) {
        return new TransaccionP2P(id, ventaId, compraId, vendedorId, compradorId,
                energia, precio, valorTotal, liquidadaEn,
                energia, energia, Energia.cero(), Energia.cero());
    }

    public TransaccionId id() { return id; }
    public OrdenId ordenVentaId() { return ordenVentaId; }
    public OrdenId ordenCompraId() { return ordenCompraId; }
    public String vendedorId() { return vendedorId; }
    public String compradorId() { return compradorId; }
    public Energia energia() { return energia; }
    public Precio precioCasacion() { return precioCasacion; }
    public BigDecimal valorTotal() { return valorTotal; }
    public Instant liquidadaEn() { return liquidadaEn; }

    public Energia energiaSolicitadaVenta() { return energiaSolicitadaVenta; }
    public Energia energiaSolicitadaCompra() { return energiaSolicitadaCompra; }
    public Energia pendienteVenta() { return pendienteVenta; }
    public Energia pendienteCompra() { return pendienteCompra; }

    /**
     * El match es TOTAL si ambas ordenes quedaron sin energia pendiente;
     * en caso contrario es PARCIAL (alguna parte no fue satisfecha del todo).
     */
    public boolean casacionTotal() {
        return pendienteVenta.esCero() && pendienteCompra.esCero();
    }

    public String tipoCasacion() {
        return casacionTotal() ? "TOTAL" : "PARCIAL";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransaccionP2P t)) return false;
        return id.equals(t.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
