package com.voltera.pagos.application;

import com.voltera.pagos.domain.exception.PagoNoEncontradoException;
import com.voltera.pagos.domain.model.Monto;
import com.voltera.pagos.domain.model.OrdenPago;
import com.voltera.pagos.domain.model.PagoId;
import com.voltera.pagos.domain.model.TipoPago;
import com.voltera.pagos.domain.port.in.ConsultarPagoUseCase;
import com.voltera.pagos.domain.port.in.ProcesarPagoUseCase;
import com.voltera.pagos.domain.port.out.PagoRepositoryPort;

import java.util.List;

/**
 * Servicio de aplicacion: orquesta el procesamiento de pagos.
 * Aplica IDEMPOTENCIA: si ya existe una orden para la misma referencia,
 * la devuelve en vez de crear una nueva (evita cobros duplicados).
 */
public class PagoService implements ProcesarPagoUseCase, ConsultarPagoUseCase {

    private final PagoRepositoryPort repositorio;

    public PagoService(PagoRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public OrdenPago procesar(ComandoProcesarPago c) {
        // Idempotencia: misma referencia => misma orden.
        var existente = repositorio.buscarPorReferencia(c.referencia());
        if (existente.isPresent()) {
            return existente.get();
        }

        TipoPago tipo = TipoPago.valueOf(c.tipo().toUpperCase());
        OrdenPago orden = OrdenPago.crear(c.prosumidorId(), c.referencia(), tipo, Monto.de(c.monto()));

        // Simulacion de la interaccion con la pasarela de pagos (PSP):
        // aqui iria el adaptador de salida hacia el PSP. Por defecto, exito.
        orden.marcarProcesada();

        return repositorio.guardar(orden);
    }

    @Override
    public OrdenPago actualizar(String pagoId, ComandoProcesarPago c) {
        PagoId id = PagoId.de(pagoId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new PagoNoEncontradoException(pagoId));

        TipoPago tipo = TipoPago.valueOf(c.tipo().toUpperCase());
        OrdenPago orden = OrdenPago.crearCon(id, c.prosumidorId(), c.referencia(), tipo, Monto.de(c.monto()));

        // Re-procesa el pago (misma logica de dominio que procesar).
        orden.marcarProcesada();

        return repositorio.guardar(orden);
    }

    @Override
    public void eliminar(String pagoId) {
        PagoId id = PagoId.de(pagoId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new PagoNoEncontradoException(pagoId));
        repositorio.eliminar(id);
    }

    @Override
    public OrdenPago porId(String pagoId) {
        return repositorio.buscarPorId(PagoId.de(pagoId))
                .orElseThrow(() -> new PagoNoEncontradoException(pagoId));
    }

    @Override
    public List<OrdenPago> porProsumidor(String prosumidorId) {
        return repositorio.buscarPorProsumidor(prosumidorId);
    }
}
