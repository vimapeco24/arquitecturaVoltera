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
    private final com.voltera.ia.infrastructure.audit.AuditoriaIa auditoria;

    public ConocimientoController(Bm25ConocimientoDer rag, com.voltera.ia.infrastructure.audit.AuditoriaIa auditoria) {
        this.rag = rag;
        this.auditoria = auditoria;
    }

    public record IngestaRequest(@jakarta.validation.constraints.NotBlank String contenido,
                                 @jakarta.validation.constraints.NotBlank String responsable) {}

    /** Ingesta de un documento (proyección): queda buscable de inmediato. */
    @org.springframework.web.bind.annotation.PostMapping("/documentos")
    public ResponseEntity<Bm25ConocimientoDer.Documento> ingerir(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody IngestaRequest req) {
        var d = rag.ingerir(req.contenido());
        auditoria.registrar("CONOCIMIENTO", req.responsable(), d.id(), "ingesta: " + d.titulo() + " · " + d.secciones() + " secciones · acceso " + d.acceso(), null);
        return ResponseEntity.ok(d);
    }

    /** Supresión propagada: el documento sale del índice. */
    @org.springframework.web.bind.annotation.DeleteMapping("/documentos/{id}")
    public ResponseEntity<Map<String, Object>> retirar(@org.springframework.web.bind.annotation.PathVariable String id,
                                                       @RequestParam(defaultValue = "consola") String responsable) {
        boolean ok = rag.retirar(id);
        if (!ok) return ResponseEntity.notFound().build();
        auditoria.registrar("CONOCIMIENTO", responsable, id, "retirado del índice", null);
        return ResponseEntity.ok(Map.of("retirado", id, "documentos", rag.documentos().size()));
    }

    @GetMapping("/documentos")
    public ResponseEntity<Map<String, Object>> documentos() {
        return ResponseEntity.ok(Map.of(
                "algoritmo", "Híbrido: BM25 (k1=1.2, b=0.75) + coseno TF-IDF, fusión RRF (k=60) y reordenamiento por cobertura; permisos PUBLICO/OPERADOR",
                "documentos", rag.documentos(),
                "fragmentos", rag.totalFragmentos()));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Bm25ConocimientoDer.Resultado>> buscar(
            @RequestParam("q") String q,
            @RequestParam(value = "tipoActivo", required = false) String tipoActivo,
            @RequestParam(value = "k", defaultValue = "5") int k,
            @RequestParam(value = "rol", defaultValue = "OPERADOR") String rol) {
        if (q == null || q.isBlank() || q.length() > 500) throw new IllegalArgumentException("Consulta vacía o demasiado larga.");
        return ResponseEntity.ok(rag.buscar(q, tipoActivo, null, Math.max(1, Math.min(k, 10)), rol));
    }
}
