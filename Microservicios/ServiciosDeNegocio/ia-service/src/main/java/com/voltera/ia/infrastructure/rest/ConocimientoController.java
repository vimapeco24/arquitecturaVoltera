package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.infrastructure.knowledge.Bm25ConocimientoDer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Knowledge Service (RAG): catálogo del corpus y búsqueda BM25 para inspeccionar lo que recupera el agente. */
@RestController
@RequestMapping("/api/v1/ia/conocimiento")
public class ConocimientoController {

    private final Bm25ConocimientoDer rag;

    public ConocimientoController(Bm25ConocimientoDer rag) {
        this.rag = rag;
    }

    @GetMapping("/documentos")
    public ResponseEntity<Map<String, Object>> documentos() {
        return ResponseEntity.ok(Map.of(
                "algoritmo", "BM25 (k1=1.2, b=0.75) sobre fragmentos por sección",
                "documentos", rag.documentos(),
                "fragmentos", rag.totalFragmentos()));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Bm25ConocimientoDer.Resultado>> buscar(
            @RequestParam("q") String q,
            @RequestParam(value = "tipoActivo", required = false) String tipoActivo,
            @RequestParam(value = "k", defaultValue = "5") int k) {
        if (q == null || q.isBlank() || q.length() > 500) throw new IllegalArgumentException("Consulta vacía o demasiado larga.");
        return ResponseEntity.ok(rag.buscar(q, tipoActivo, null, Math.max(1, Math.min(k, 10))));
    }
}
