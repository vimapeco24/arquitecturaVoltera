package com.voltera.liquidacionmensual.domain.port.in;

import com.voltera.liquidacionmensual.domain.model.LiquidacionMensual;

import java.util.List;

public interface ConsultarLiquidacionUseCase {
    LiquidacionMensual porId(String liquidacionId);
    List<LiquidacionMensual> porProsumidor(String prosumidorId);
}
