package com.voltera.telemetria.domain.port.out;

import com.voltera.telemetria.domain.model.ConsumoRegistrado;

import java.util.List;

/**
 * Puerto de SALIDA: buffer local de eventos para el patron
 * <b>store-and-forward</b> del componente IoT.
 *
 * <p>Cuando el medidor no tiene conexion con el broker, los eventos
 * {@link ConsumoRegistrado} se <b>encolan</b> localmente aqui. Al recuperar la
 * conexion, un proceso los <b>desencola</b> (drena) y los reenvia al broker.
 * Asi ninguna lectura se pierde por una caida de red.</p>
 */
public interface BufferOfflinePort {

    /** Encola un evento que no se pudo entregar al broker. */
    void encolar(ConsumoRegistrado evento);

    /** Devuelve los eventos pendientes de reenvio (FIFO), sin removerlos. */
    List<ConsumoRegistrado> pendientes();

    /** Remueve un evento del buffer una vez confirmada su entrega. */
    void confirmarEntrega(ConsumoRegistrado evento);

    /** Cantidad de eventos pendientes de reenvio. */
    int tamano();
}
