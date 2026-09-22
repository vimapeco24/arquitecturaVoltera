package com.voltera.liquidacion.application;

import com.voltera.liquidacion.domain.exception.TransaccionNoEncontradaException;
import com.voltera.liquidacion.domain.model.Energia;
import com.voltera.liquidacion.domain.model.OrdenId;
import com.voltera.liquidacion.domain.model.OrdenMercado;
import com.voltera.liquidacion.domain.model.Precio;
import com.voltera.liquidacion.domain.model.TipoOrden;
import com.voltera.liquidacion.domain.model.TransaccionId;
import com.voltera.liquidacion.domain.model.TransaccionP2P;
import com.voltera.liquidacion.domain.port.in.ConsultarTransaccionUseCase;
import com.voltera.liquidacion.domain.port.in.EmparejarOrdenesUseCase;
import com.voltera.liquidacion.domain.port.out.TransaccionRepositoryPort;

import java.util.List;

/**
 * Servicio de aplicacion: orquesta el emparejamiento y la liquidacion P2P.
 * No contiene reglas de negocio (viven en el dominio), solo coordina.
 * Agnostico de framework.
 */
public class LiquidacionService implements EmparejarOrdenesUseCase, ConsultarTransaccionUseCase {

    private final TransaccionRepositoryPort repositorio;

    public LiquidacionService(TransaccionRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public TransaccionP2P emparejar(ComandoEmparejar c) {
        OrdenMercado venta = OrdenMercado.crear(
                OrdenId.nuevo(), c.vendedorId(), TipoOrden.VENTA,
                Energia.deKwh(c.kwhVenta()), Precio.porKwh(c.precioVenta()));

        OrdenMercado compra = OrdenMercado.crear(
                OrdenId.nuevo(), c.compradorId(), TipoOrden.COMPRA,
                Energia.deKwh(c.kwhCompra()), Precio.porKwh(c.precioCompra()));

        TransaccionP2P transaccion = TransaccionP2P.liquidar(TransaccionId.nuevo(), venta, compra);
        return repositorio.guardar(transaccion);
    }

    @Override
    public TransaccionP2P actualizar(String transaccionId, ComandoEmparejar c) {
        TransaccionId id = TransaccionId.de(transaccionId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new TransaccionNoEncontradaException(transaccionId));

        OrdenMercado venta = OrdenMercado.crear(
                OrdenId.nuevo(), c.vendedorId(), TipoOrden.VENTA,
                Energia.deKwh(c.kwhVenta()), Precio.porKwh(c.precioVenta()));

        OrdenMercado compra = OrdenMercado.crear(
                OrdenId.nuevo(), c.compradorId(), TipoOrden.COMPRA,
                Energia.deKwh(c.kwhCompra()), Precio.porKwh(c.precioCompra()));

        TransaccionP2P transaccion = TransaccionP2P.liquidar(id, venta, compra);
        return repositorio.guardar(transaccion);
    }

    @Override
    public void eliminar(String transaccionId) {
        TransaccionId id = TransaccionId.de(transaccionId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new TransaccionNoEncontradaException(transaccionId));
        repositorio.eliminar(id);
    }

    @Override
    public TransaccionP2P porId(String transaccionId) {
        return repositorio.buscarPorId(TransaccionId.de(transaccionId))
                .orElseThrow(() -> new TransaccionNoEncontradaException(transaccionId));
    }

    @Override
    public List<TransaccionP2P> porProsumidor(String prosumidorId) {
        return repositorio.buscarPorProsumidor(prosumidorId);
    }
}
