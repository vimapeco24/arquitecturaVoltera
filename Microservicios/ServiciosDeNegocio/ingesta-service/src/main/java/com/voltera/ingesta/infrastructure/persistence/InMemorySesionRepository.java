package com.voltera.ingesta.infrastructure.persistence;

import com.voltera.ingesta.domain.model.ReglaDeValidacion;
import com.voltera.ingesta.domain.model.SesionDeIngesta;
import com.voltera.ingesta.domain.port.out.SesionRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * Mantiene una unica sesion de ingesta en memoria (una por nodo).
 */
@Repository
public class InMemorySesionRepository implements SesionRepositoryPort {

    private final SesionDeIngesta sesion;

    public InMemorySesionRepository(@Value("${ingesta.validacion.min-kwh:0.0}") double minKwh,
                                    @Value("${ingesta.validacion.max-kwh:100.0}") double maxKwh) {
        this.sesion = new SesionDeIngesta(new ReglaDeValidacion(minKwh, maxKwh));
    }

    @Override
    public SesionDeIngesta obtener() {
        return sesion;
    }
}
