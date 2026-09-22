package com.voltera.pagos.domain.port.in;

import com.voltera.pagos.domain.model.OrdenPago;

import java.util.List;

public interface ConsultarPagoUseCase {
    OrdenPago porId(String pagoId);
    List<OrdenPago> porProsumidor(String prosumidorId);
}
