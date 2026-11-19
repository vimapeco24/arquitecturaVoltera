package com.voltera.blockchain.infrastructure.rest;

import com.voltera.blockchain.domain.model.LecturaGobernanza;
import com.voltera.blockchain.domain.port.in.ConsultarGobernanzaUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lectura on-chain (solo GET) de las aprobaciones múltiples del contrato MultiFirmaAccionDER. */
@RestController
@RequestMapping("/api/v1/gobernanza")
public class GobernanzaLecturaController {

    private final ConsultarGobernanzaUseCase consultar;

    public GobernanzaLecturaController(ConsultarGobernanzaUseCase consultar) {
        this.consultar = consultar;
    }

    @GetMapping("/propuestas/{id}/estado")
    public LecturaGobernanza estado(@PathVariable String id) {
        return consultar.leer(id);
    }
}
