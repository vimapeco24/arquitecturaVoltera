package com.voltera.certificacion.infrastructure.persistence;

import com.voltera.certificacion.domain.model.BalanceGeneracionCertificable;
import com.voltera.certificacion.domain.model.SolicitudCertificacion;
import com.voltera.certificacion.domain.port.out.SolicitudRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Persistencia OFF-CHAIN en memoria (patrón del proyecto). */
public class InMemorySolicitudRepository implements SolicitudRepositoryPort {

    private final Map<String, BalanceGeneracionCertificable> balances = new ConcurrentHashMap<>();
    private final Map<String, SolicitudCertificacion> solicitudes = new ConcurrentHashMap<>();

    @Override
    public BalanceGeneracionCertificable guardarBalance(BalanceGeneracionCertificable balance) {
        balances.put(balance.medidorId(), balance);
        return balance;
    }

    @Override
    public Optional<BalanceGeneracionCertificable> buscarBalance(String medidorId) {
        return Optional.ofNullable(balances.get(medidorId));
    }

    @Override
    public List<BalanceGeneracionCertificable> todosLosBalances() {
        return new ArrayList<>(balances.values());
    }

    @Override
    public SolicitudCertificacion guardarSolicitud(SolicitudCertificacion solicitud) {
        solicitudes.put(solicitud.id(), solicitud);
        return solicitud;
    }

    @Override
    public Optional<SolicitudCertificacion> buscarSolicitud(String id) {
        return Optional.ofNullable(solicitudes.get(id));
    }

    @Override
    public List<SolicitudCertificacion> todasLasSolicitudes() {
        return new ArrayList<>(solicitudes.values());
    }
}
