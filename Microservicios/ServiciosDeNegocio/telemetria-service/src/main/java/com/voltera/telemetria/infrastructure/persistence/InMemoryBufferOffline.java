package com.voltera.telemetria.infrastructure.persistence;

import com.voltera.telemetria.domain.model.ConsumoRegistrado;
import com.voltera.telemetria.domain.port.out.BufferOfflinePort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Adaptador del buffer offline en memoria (store-and-forward).
 *
 * <p>Usa una cola FIFO concurrente. En un despliegue real este buffer viviria en
 * el borde (edge) del medidor IoT, persistido en disco/SQLite para sobrevivir a
 * reinicios; aqui se modela en memoria para la demo academica.</p>
 */
@Repository
public class InMemoryBufferOffline implements BufferOfflinePort {

    private final ConcurrentLinkedQueue<ConsumoRegistrado> cola = new ConcurrentLinkedQueue<>();

    @Override
    public void encolar(ConsumoRegistrado evento) {
        cola.add(evento);
    }

    @Override
    public List<ConsumoRegistrado> pendientes() {
        return List.copyOf(cola);
    }

    @Override
    public void confirmarEntrega(ConsumoRegistrado evento) {
        cola.remove(evento);
    }

    @Override
    public int tamano() {
        return cola.size();
    }
}
