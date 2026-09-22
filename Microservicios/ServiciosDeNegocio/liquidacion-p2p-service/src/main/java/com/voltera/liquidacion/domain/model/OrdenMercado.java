package com.voltera.liquidacion.domain.model;

import java.util.Objects;

/**
 * Entidad OrdenMercado: intencion de compra o venta de energia publicada
 * por un prosumidor. Mantiene la energia pendiente por casar.
 */
public class OrdenMercado {

    private final OrdenId id;
    private final String prosumidorId;
    private final TipoOrden tipo;
    private final Energia cantidad;
    private final Precio precioLimite;
    private Energia pendiente;

    private OrdenMercado(OrdenId id, String prosumidorId, TipoOrden tipo,
                         Energia cantidad, Precio precioLimite, Energia pendiente) {
        this.id = id;
        this.prosumidorId = prosumidorId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.precioLimite = precioLimite;
        this.pendiente = pendiente;
    }

    public static OrdenMercado crear(OrdenId id, String prosumidorId, TipoOrden tipo,
                                     Energia cantidad, Precio precioLimite) {
        Objects.requireNonNull(id, "id requerido");
        if (prosumidorId == null || prosumidorId.isBlank()) {
            throw new IllegalArgumentException("prosumidorId requerido");
        }
        Objects.requireNonNull(tipo, "tipo requerido");
        Objects.requireNonNull(cantidad, "cantidad requerida");
        Objects.requireNonNull(precioLimite, "precioLimite requerido");
        if (cantidad.esCero()) {
            throw new IllegalArgumentException("La cantidad de la orden debe ser mayor a cero");
        }
        return new OrdenMercado(id, prosumidorId, tipo, cantidad, precioLimite, cantidad);
    }

    /** Reduce la energia pendiente tras un emparejamiento parcial o total. */
    void reducirPendiente(Energia casada) {
        if (!pendiente.mayorOIgualQue(casada)) {
            throw new IllegalStateException("No se puede casar mas energia de la pendiente");
        }
        this.pendiente = pendiente.restar(casada);
    }

    public boolean estaCompleta() {
        return pendiente.esCero();
    }

    public OrdenId id() { return id; }
    public String prosumidorId() { return prosumidorId; }
    public TipoOrden tipo() { return tipo; }
    public Energia cantidad() { return cantidad; }
    public Precio precioLimite() { return precioLimite; }
    public Energia pendiente() { return pendiente; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrdenMercado om)) return false;
        return id.equals(om.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
