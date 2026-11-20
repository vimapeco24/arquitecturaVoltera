package com.voltera.ia.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/** Acceso a la identidad verificada de la solicitud en curso (vacía cuando IAM está deshabilitado). */
public final class IamContexto {

    public static final String ATTR = "voltera.iam.identidad";

    private IamContexto() {}

    public static Optional<JwtVerificador.Identidad> actual() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        Object o = ra == null ? null : ra.getAttribute(ATTR, RequestAttributes.SCOPE_REQUEST);
        return o instanceof JwtVerificador.Identidad i ? Optional.of(i) : Optional.empty();
    }

    /** Con IAM activo la identidad es la del token; si no, la que declara el cliente. */
    public static String identidadO(String declarada) {
        return actual().map(JwtVerificador.Identidad::sujeto).orElse(declarada);
    }

    /** Mínimo privilegio: con IAM activo, el token debe traer el rol con el que se firma. */
    public static void exigirRol(String rol) {
        actual().ifPresent(i -> {
            if (!i.roles().contains(rol.toUpperCase())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "el token de '" + i.sujeto() + "' no tiene el rol " + rol);
            }
        });
    }
}
