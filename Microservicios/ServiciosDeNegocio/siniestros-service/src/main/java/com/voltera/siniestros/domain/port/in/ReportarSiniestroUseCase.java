package com.voltera.siniestros.domain.port.in;

import com.voltera.siniestros.domain.model.Siniestro;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Puerto de ENTRADA: reporta un nuevo siniestro (nace en estado REPORTADO).
 */
public interface ReportarSiniestroUseCase {

    Siniestro reportar(ComandoReportarSiniestro comando);

    void eliminar(String siniestroId);

    record ComandoReportarSiniestro(
            String polizaId,
            String prosumidorId,
            String descripcion,
            BigDecimal montoReclamacion,
            LocalDate fechaOcurrencia
    ) {}
}
