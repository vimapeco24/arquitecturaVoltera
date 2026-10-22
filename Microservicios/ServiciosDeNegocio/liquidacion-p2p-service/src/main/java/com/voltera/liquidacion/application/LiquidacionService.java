package com.voltera.liquidacion.application;

import com.voltera.liquidacion.domain.exception.TransaccionNoEncontradaException;
import com.voltera.liquidacion.domain.model.Energia;
import com.voltera.liquidacion.domain.model.LiquidacionCalculada;
import com.voltera.liquidacion.domain.model.OrdenId;
import com.voltera.liquidacion.domain.model.OrdenMercado;
import com.voltera.liquidacion.domain.model.Precio;
import com.voltera.liquidacion.domain.model.TipoOrden;
import com.voltera.liquidacion.domain.model.TransaccionId;
import com.voltera.liquidacion.domain.model.TransaccionP2P;
import com.voltera.liquidacion.domain.port.in.ConsultarTransaccionUseCase;
import com.voltera.liquidacion.domain.port.in.EmparejarOrdenesUseCase;
import com.voltera.liquidacion.domain.port.out.AnclajeBlockchainPort;
import com.voltera.liquidacion.domain.port.out.LiquidacionEventPublisherPort;
import com.voltera.liquidacion.domain.port.out.TransaccionRepositoryPort;

import java.util.List;

/**
 * Servicio de aplicacion: orquesta el emparejamiento y la liquidacion P2P.
 * No contiene reglas de negocio (viven en el dominio), solo coordina.
 * Agnostico de framework.
 *
 * <p>Al casar, hace DOS cosas con la transacción liquidada:
 * <ol>
 *   <li>Solicita el anclaje por REST al blockchain-gateway (best-effort, el
 *       camino que funciona hoy en Render sin Kafka).</li>
 *   <li>Publica el evento {@link LiquidacionCalculada} en el event mesh
 *       (si el EDA está habilitado); el gateway lo consume y ancla solo.
 *       Esto cierra la orquestación por eventos del diagrama.</li>
 * </ol>
 * Ambos caminos son idempotentes aguas abajo (el gateway deduplica por
 * transaccionId), así que tener los dos activos no duplica anclajes.</p>
 */
public class LiquidacionService implements EmparejarOrdenesUseCase, ConsultarTransaccionUseCase {

    private final TransaccionRepositoryPort repositorio;
    private final AnclajeBlockchainPort anclajeBlockchain;
    private final LiquidacionEventPublisherPort eventPublisher;

    public LiquidacionService(TransaccionRepositoryPort repositorio,
                              AnclajeBlockchainPort anclajeBlockchain,
                              LiquidacionEventPublisherPort eventPublisher) {
        this.repositorio = repositorio;
        this.anclajeBlockchain = anclajeBlockchain;
        this.eventPublisher = eventPublisher;
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
        TransaccionP2P guardada = repositorio.guardar(transaccion);
        // (1) Solicita el anclaje en la cadena (no-op si el gateway no esta configurado).
        anclajeBlockchain.solicitarAnclaje(guardada);
        // (2) Publica el evento EDA (no-op si Kafka esta deshabilitado).
        eventPublisher.publicar(LiquidacionCalculada.de(guardada, c.comunidadId()));
        return guardada;
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
        TransaccionP2P guardada = repositorio.guardar(transaccion);
        anclajeBlockchain.solicitarAnclaje(guardada);
        eventPublisher.publicar(LiquidacionCalculada.de(guardada, c.comunidadId()));
        return guardada;
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

    @Override
    public List<TransaccionP2P> todas() {
        return repositorio.buscarTodas();
    }
}
