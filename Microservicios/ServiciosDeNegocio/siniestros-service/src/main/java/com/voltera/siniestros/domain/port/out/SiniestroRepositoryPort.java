package com.voltera.siniestros.domain.port.out;

import com.voltera.siniestros.domain.model.Siniestro;
import com.voltera.siniestros.domain.model.SiniestroId;

import java.util.List;
import java.util.Optional;

public interface SiniestroRepositoryPort {
    Siniestro guardar(Siniestro siniestro);
    Optional<Siniestro> buscarPorId(SiniestroId id);
    List<Siniestro> buscarPorPoliza(String polizaId);
    List<Siniestro> buscarTodos();
    void eliminar(SiniestroId id);
}
