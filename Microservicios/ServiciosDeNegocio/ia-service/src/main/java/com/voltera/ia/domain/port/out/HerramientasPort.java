package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.SolicitudCopiloto;

import java.util.List;

/**
 * Puerto de salida hacia las Herramientas (MCP sobre las APIs existentes): el
 * agente llama a los microservicios como herramientas, con minimo privilegio y
 * nunca directo a las bases de datos. Devuelve evidencia citable: estado del
 * siniestro (siniestros-service) y riesgo cedido en la cadena (blockchain).
 *
 * <p>El copiloto consulta y lee; NO tiene la herramienta para aprobar pagos:
 * esa accion la ejecuta el analista en su consola (human-in-the-loop).</p>
 */
public interface HerramientasPort {

    List<Evidencia> consultar(SolicitudCopiloto solicitud);
}
