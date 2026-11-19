package com.voltera.ia.infrastructure.evals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.der.TipoActivo;
import com.voltera.ia.domain.model.evals.CasoEval;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Carga el dataset etiquetado de Evals desde {@code classpath:evals/casos-der.json}. */
@Component
public class DatasetEvalsLoader {

    private final List<CasoEval> casos = new ArrayList<>();
    private final String nombre;

    public DatasetEvalsLoader() {
        try (InputStream in = new ClassPathResource("evals/casos-der.json").getInputStream()) {
            JsonNode raiz = new ObjectMapper().readTree(in);
            nombre = raiz.path("dataset").asText("dataset") + "@" + raiz.path("version").asText("1");
            for (JsonNode c : raiz.path("casos")) {
                JsonNode e = c.path("esperado");
                casos.add(new CasoEval(c.path("id").asText(), c.path("descripcion").asText(),
                        TipoActivo.valueOf(c.path("tipoActivo").asText()), c.path("metrica").asText(),
                        new BigDecimal(c.path("valor").asText("0")), new BigDecimal(c.path("umbral").asText("0")),
                        c.path("minutosSinSenal").asInt(0),
                        Severidad.valueOf(e.path("severidad").asText()), TipoAccion.valueOf(e.path("accion").asText()),
                        e.path("requiereHumano").asBoolean(), c.path("nota").asText(""), CasoEval.DATASET));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cargar el dataset de Evals", ex);
        }
    }

    public List<CasoEval> casos() { return List.copyOf(casos); }

    public String nombre() { return nombre; }
}
