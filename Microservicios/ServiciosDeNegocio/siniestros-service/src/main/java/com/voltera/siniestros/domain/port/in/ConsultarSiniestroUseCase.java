package com.voltera.siniestros.domain.port.in;

import com.voltera.siniestros.domain.model.Siniestro;

import java.util.List;

public interface ConsultarSiniestroUseCase {
    Siniestro porId(String siniestroId);
    List<Siniestro> porPoliza(String polizaId);
    List<Siniestro> todos();
}
