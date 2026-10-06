package com.voltera.notificaciones.domain.port.out;

import com.voltera.notificaciones.domain.model.Preferencia;

/**
 * Puerto de salida: resuelve la {@link Preferencia} de notificacion asociada a un
 * medidor/cliente. Si no hay preferencia registrada, se usa la de por defecto.
 */
public interface PreferenciaRepositoryPort {
    Preferencia preferenciaDe(String medidorId);
    void registrar(String medidorId, Preferencia preferencia);
}
