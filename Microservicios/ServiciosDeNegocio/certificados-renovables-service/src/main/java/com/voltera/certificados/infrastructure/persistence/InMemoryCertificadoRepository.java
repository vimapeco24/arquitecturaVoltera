package com.voltera.certificados.infrastructure.persistence;

import com.voltera.certificados.domain.model.CertificadoRenovable;
import com.voltera.certificados.domain.port.out.CertificadoRepositoryPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Persistencia OFF-CHAIN en memoria (patrón del proyecto). */
public class InMemoryCertificadoRepository implements CertificadoRepositoryPort {

    private final Map<String, CertificadoRenovable> store = new ConcurrentHashMap<>();

    @Override
    public CertificadoRenovable guardar(CertificadoRenovable certificado) {
        store.put(certificado.id(), certificado);
        return certificado;
    }

    @Override
    public Optional<CertificadoRenovable> buscar(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<CertificadoRenovable> todos() {
        List<CertificadoRenovable> lista = new ArrayList<>(store.values());
        lista.sort(Comparator.comparing(CertificadoRenovable::creadoEn).reversed());
        return lista;
    }
}
