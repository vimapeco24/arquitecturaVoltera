package com.voltera.ia.infrastructure.audit;

import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.port.out.AuditLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Caja 8 · Audit &amp; Retention. Registro de auditoría de cada diagnóstico: quién
 * lo disparó, sobre qué activo, qué acción se propuso, si exigió aprobación
 * humana y con qué versión de prompt/modelo. En esta demostración se escribe al
 * log estructurado (retención gestionada por la plataforma de observabilidad);
 * la interfaz permite enchufar un almacén dedicado sin tocar al agente.
 */
@Component
public class LogAuditLog implements AuditLogPort {

    private static final Logger audit = LoggerFactory.getLogger("VOLTERA.AUDIT.IA");

    @Override
    public void registrar(DiagnosticoDer d) {
        audit.info("diagnosticoId={} usuario={} activo={} tipo={} comunidad={} severidad={} "
                        + "accionPropuesta={} requiereAprobacionHumana={} motor={} prompt={} datosEnmascarados={} ts={}",
                d.diagnosticoId(), d.usuario(), d.activoId(), d.tipoActivo(), d.comunidadId(),
                d.severidad(), d.accionPropuesta(), d.requiereAprobacionHumana(), d.motor(),
                d.promptVersion(), d.datosEnmascarados(), d.generadoEn());
    }
}
