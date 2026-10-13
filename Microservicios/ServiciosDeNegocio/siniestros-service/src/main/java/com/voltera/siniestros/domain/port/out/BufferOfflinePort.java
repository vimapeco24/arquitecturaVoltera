package com.voltera.siniestros.domain.port.out;

import com.voltera.siniestros.domain.model.SiniestroAprobado;

import java.util.List;

/**
 * Puerto de SALIDA: buffer local de eventos para el patron
 * <b>store-and-forward</b> del productor de Siniestros.
 *
 * <p>Cuando el broker no esta disponible al aprobar un siniestro, el evento
 * {@link SiniestroAprobado} se <b>encola</b> localmente aqui. Al recuperar la
 * conexion, un proceso los <b>desencola</b> (drena) y los reenvia al broker.
 * Asi ningun evento de aprobacion se pierde por una caida del broker.</p>
 */
public interface BufferOfflinePort {

    /** Encola un evento que no se pudo entregar al broker. */
    void encolar(SiniestroAprobado evento);

    /** Devuelve los eventos pendientes de reenvio (FIFO), sin removerlos. */
    List<SiniestroAprobado> pendientes();

    /** Remueve un evento del buffer una vez confirmada su entrega. */
    void confirmarEntrega(SiniestroAprobado evento);

    /** Cantidad de eventos pendientes de reenvio. */
    int tamano();
}
