package com.voltera.ia.domain.port.in;

import com.voltera.ia.domain.model.RecomendacionCopiloto;
import com.voltera.ia.domain.model.SolicitudCopiloto;

/**
 * Puerto de entrada: ejecutar el flujo agentico acotado del Copiloto de
 * Siniestros sobre un caso y devolver una recomendacion (propuesta).
 */
public interface CopilotoSiniestrosUseCase {

    RecomendacionCopiloto analizar(SolicitudCopiloto solicitud);
}
