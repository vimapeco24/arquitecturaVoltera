package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.der.DiagnosticoDer;

/**
 * Caja 8 · Audit &amp; Retention. Qué se registra, con qué redacción y por cuánto
 * tiempo. Registra cada diagnóstico y su propuesta con la identidad de quien lo
 * disparó, la versión de prompt/modelo y si exigió aprobación humana, para poder
 * reconstruir ante el regulador por qué se propuso una acción.
 */
public interface AuditLogPort {

    void registrar(DiagnosticoDer diagnostico);
}
