package com.voltera.blockchain.infrastructure.blockchain;

import com.voltera.blockchain.domain.model.LecturaGobernanza;
import com.voltera.blockchain.domain.port.out.ContratoGobernanzaPort;

import java.util.List;
import java.util.Optional;

/** Adaptador nulo: sin RPC/dirección configurados la lectura es SIMULADA. */
public class ContratoGobernanzaNoConfigurado implements ContratoGobernanzaPort {
    @Override public boolean configurado() { return false; }
    @Override public String direccionContrato() { return null; }
    @Override public Optional<PropuestaOnChain> obtenerPropuesta(String id) { return Optional.empty(); }
    @Override public List<LecturaGobernanza.FirmaLeida> firmas(String id) { return List.of(); }
}
