package com.voltera.ia.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Caja 7 · Identity (IAM). Con {@code ia.iam.enabled=true} toda llamada a /api/** exige un Bearer JWT válido
 * y la identidad sale del token (no de lo que diga el cuerpo). Sin esa bandera el servicio se comporta como
 * antes (demo abierta). Exentos: preflight CORS, /actuator y /api/v1/ia/estado.
 */
@Component
@ConditionalOnProperty(name = "ia.iam.enabled", havingValue = "true")
public class IamFilter extends OncePerRequestFilter {

    private final JwtVerificador verificador;

    public IamFilter(@Value("${ia.iam.algoritmo:HS256}") String algoritmo,
                     @Value("${ia.iam.hs256-secret:}") String secreto,
                     @Value("${ia.iam.rs256-public-key:}") String clavePublica,
                     @Value("${ia.iam.issuer:}") String emisor) {
        this.verificador = "RS256".equalsIgnoreCase(algoritmo)
                ? JwtVerificador.rs256(clavePublica, emisor)
                : JwtVerificador.hs256(secreto, emisor);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        String p = r.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(r.getMethod()) || !p.startsWith("/api/")
                || p.equals("/api/v1/ia/estado");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h == null || !h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            rechazar(res, "token requerido");
            return;
        }
        try {
            req.setAttribute(IamContexto.ATTR, verificador.verificar(h.substring(7).trim()));
        } catch (JwtVerificador.TokenInvalido e) {
            rechazar(res, "token inválido (" + e.getMessage() + ")");
            return;
        }
        chain.doFilter(req, res);
    }

    private static void rechazar(HttpServletResponse res, String motivo) throws IOException {
        res.setStatus(401);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"" + motivo + "\"}");
    }
}
