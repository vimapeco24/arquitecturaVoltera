package com.voltera.siniestros.infrastructure.persistence;

import com.voltera.siniestros.domain.model.SiniestroAprobado;
import com.voltera.siniestros.domain.port.out.BufferOfflinePort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Adaptador del buffer offline en memoria (store-and-forward).
 *
 * <p>Usa una cola FIFO concurrente. Cuando el broker no confirma la entrega del
 * evento {@link SiniestroAprobado}, el evento se guarda aqui y se reenvia al
 * recuperar la conexion. En un despliegue real este buffer estaria persistido
 * (disco/tabla outbox) para sobrevivir a reinicios; aqui se modela en memoria
 * para la demo academica.</p>
 */
@Repository
public class InMemoryBufferOffline implements BufferOfflinePort {

    private final ConcurrentLinkedQueue<SiniestroAprobado> cola = new ConcurrentLinkedQueue<>();

    @Override
    public void encolar(SiniestroAprobado evento) {
        cola.add(evento);
    }

    @Override
    public List<SiniestroAprobado> pendientes() {
        return List.copyOf(cola);
    }

    @Override
    public void confirmarEntrega(SiniestroAprobado evento) {
        cola.remove(evento);
    }

    @Override
    public int tamano() {
        return cola.size();
    }
}
