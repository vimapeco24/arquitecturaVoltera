package com.voltera.facturacion.domain.port.out;

import com.voltera.facturacion.domain.model.Factura;
import com.voltera.facturacion.domain.model.FacturaId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de SALIDA (driven port): el dominio define que necesita persistir,
 * sin conocer la tecnologia. Los adaptadores (BD, memoria) lo implementan.
 */
public interface FacturaRepositoryPort {

    Factura guardar(Factura factura);

    Optional<Factura> buscarPorId(FacturaId id);

    List<Factura> buscarPorProsumidor(String prosumidorId);

    void eliminar(FacturaId id);
}
