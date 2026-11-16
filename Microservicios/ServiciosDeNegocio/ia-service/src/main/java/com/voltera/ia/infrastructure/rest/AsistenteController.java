package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.domain.model.asistente.RespuestaAsistente;
import com.voltera.ia.domain.port.in.AsistenteUseCase;
import com.voltera.ia.infrastructure.rest.dto.AsistenteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del Assistant: conversación acotada al diagnóstico antes de firmar. No
 * expone ninguna operación de firma ni de ejecución: el Assistant informa, no
 * decide. La firma vive en {@link GobernanzaController}, deliberadamente separada.
 */
@RestController
@RequestMapping("/api/v1/ia/asistente")
public class AsistenteController {

    private final AsistenteUseCase asistente;

    public AsistenteController(AsistenteUseCase asistente) {
        this.asistente = asistente;
    }

    @PostMapping("/preguntar")
    public ResponseEntity<RespuestaAsistente> preguntar(@Valid @RequestBody AsistenteRequest req) {
        return ResponseEntity.ok(asistente.responder(req.aDominio()));
    }
}
