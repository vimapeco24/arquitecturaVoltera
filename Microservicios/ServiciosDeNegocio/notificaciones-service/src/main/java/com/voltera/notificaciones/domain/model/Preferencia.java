package com.voltera.notificaciones.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * VO Preferencia (lamina 02): define el canal preferido del cliente y a que
 * tipos de alerta esta suscrito (opt-in). Inmutable.
 */
public final class Preferencia {

    private final CanalNotificacion canalPreferido;
    private final Set<TipoAlerta> suscripciones;

    public Preferencia(CanalNotificacion canalPreferido, Set<TipoAlerta> suscripciones) {
        if (canalPreferido == null) {
            throw new IllegalArgumentException("El canal preferido es obligatorio");
        }
        this.canalPreferido = canalPreferido;
        this.suscripciones = (suscripciones == null || suscripciones.isEmpty())
                ? EnumSet.allOf(TipoAlerta.class)
                : EnumSet.copyOf(suscripciones);
    }

    /** Preferencia por defecto: EMAIL y suscrito a todas las alertas. */
    public static Preferencia porDefecto() {
        return new Preferencia(CanalNotificacion.EMAIL, EnumSet.allOf(TipoAlerta.class));
    }

    public boolean aceptaAlerta(TipoAlerta tipo) {
        return suscripciones.contains(tipo);
    }

    public CanalNotificacion canalPreferido() {
        return canalPreferido;
    }

    public Set<TipoAlerta> suscripciones() {
        return EnumSet.copyOf(suscripciones);
    }
}
