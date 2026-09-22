package com.voltera.pagos.domain.port.out;

import com.voltera.pagos.domain.model.OrdenPago;
import com.voltera.pagos.domain.model.PagoId;

import java.util.List;
import java.util.Optional;

public interface PagoRepositoryPort {
    OrdenPago guardar(OrdenPago orden);
    Optional<OrdenPago> buscarPorId(PagoId id);
    /** Soporta idempotencia: busca una orden existente por su referencia externa. */
    Optional<OrdenPago> buscarPorReferencia(String referencia);
    List<OrdenPago> buscarPorProsumidor(String prosumidorId);
    void eliminar(PagoId id);
}
