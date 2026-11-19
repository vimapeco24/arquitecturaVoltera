package com.voltera.ia.infrastructure.observabilidad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Mide cada llamada a /api/v1/ia/** y la asigna a una funcionalidad. */
@Component
public class ObservabilidadInterceptor implements HandlerInterceptor {

    private static final String T0 = "ia.t0";
    private final ObservabilidadIa obs;

    public ObservabilidadInterceptor(ObservabilidadIa obs) {
        this.obs = obs;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        req.setAttribute(T0, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        Object t0 = req.getAttribute(T0);
        if (!(t0 instanceof Long inicio) || "OPTIONS".equals(req.getMethod())) return;
        String f = funcionalidad(req.getMethod(), req.getRequestURI());
        if (f.equals("observabilidad")) return;
        obs.registrar(f, (System.nanoTime() - inicio) / 1_000_000, ex != null || res.getStatus() >= 500);
    }

    static String funcionalidad(String metodo, String uri) {
        String u = uri.replaceFirst("^.*/api/v1/ia", "");
        if (u.startsWith("/diagnostico/der")) return "diagnostico";
        if (u.matches("/diagnostico/[^/]+/retroalimentacion")) return "human-review";
        if (u.startsWith("/asistente")) return "assistant";
        if (u.matches("/gobernanza/propuestas/[^/]+/(firmas|rechazos)/.*")) return "gobernanza-firma";
        if (u.matches("/gobernanza/propuestas/[^/]+") && "POST".equals(metodo)) return "gobernanza-propuesta";
        if (u.startsWith("/gobernanza")) return "gobernanza-consulta";
        if (u.startsWith("/evals")) return "evals";
        if (u.startsWith("/conocimiento")) return "rag";
        if (u.startsWith("/notificaciones")) return "notificaciones";
        if (u.startsWith("/prompts")) return "prompt-registry";
        if (u.startsWith("/observabilidad") || u.startsWith("/model-gateway")) return "observabilidad";
        if (u.startsWith("/copilotos")) return "copiloto-siniestros";
        return "otros";
    }
}
