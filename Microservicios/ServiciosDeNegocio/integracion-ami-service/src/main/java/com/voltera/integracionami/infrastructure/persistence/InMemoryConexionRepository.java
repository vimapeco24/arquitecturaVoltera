package com.voltera.integracionami.infrastructure.persistence;

import com.voltera.integracionami.domain.model.ConexionHeadEnd;
import com.voltera.integracionami.domain.model.Protocolo;
import com.voltera.integracionami.domain.port.out.ConexionRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * Mantiene una unica conexion head-end (proveedor principal) en memoria.
 */
@Repository
public class InMemoryConexionRepository implements ConexionRepositoryPort {

    private volatile ConexionHeadEnd conexion;

    public InMemoryConexionRepository(@Value("${ami.proveedor:Landis+Gyr}") String proveedor) {
        this.conexion = new ConexionHeadEnd(proveedor, Protocolo.MQTT);
    }

    @Override
    public ConexionHeadEnd obtener() {
        return conexion;
    }

    @Override
    public ConexionHeadEnd guardar(ConexionHeadEnd c) {
        this.conexion = c;
        return c;
    }
}
