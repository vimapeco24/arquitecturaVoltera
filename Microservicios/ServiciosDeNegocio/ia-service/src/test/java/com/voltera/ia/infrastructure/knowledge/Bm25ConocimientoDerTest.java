package com.voltera.ia.infrastructure.knowledge;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.TipoActivo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RAG BM25 sobre el corpus real de classpath:conocimiento. */
class Bm25ConocimientoDerTest {

    private final Bm25ConocimientoDer rag = new Bm25ConocimientoDer();

    @Test
    void cargaElCorpusCompleto() {
        assertEquals(12, rag.documentos().size());
        assertTrue(rag.totalFragmentos() >= 30);
    }

    @Test
    void sobretensionDeInversorRecuperaElFalloCorrecto() {
        var r = rag.buscar("inversor sobretensión sostenida voltaje", "INVERSOR", "fallos-conocidos", 1);
        assertEquals("inversor-fallos", r.get(0).docId());
        assertTrue(r.get(0).seccion().toLowerCase().contains("sobretensi"));
        assertTrue(r.get(0).score() > 0);
    }

    @Test
    void filtraPorTipoDeActivo() {
        var r = rag.buscar("sobretensión inversor", "MEDIDOR", null, 10);
        assertFalse(r.stream().anyMatch(x -> x.docId().startsWith("inversor")));
    }

    @Test
    void elDiagnosticoCitaFichaFalloYProcedimientoConDocumento() {
        List<Evidencia> ev = rag.recuperar(new AlertaTelemetria("MED-1", TipoActivo.MEDIDOR, "C", "latencia",
                BigDecimal.ZERO, BigDecimal.ZERO, 30, "", "op"));
        assertEquals(List.of("RAG:ficha-tecnica", "RAG:fallos-conocidos", "RAG:procedimiento-campo"),
                ev.stream().map(Evidencia::fuente).toList());
        assertTrue(ev.get(1).detalle().contains("[doc medidor-fallos]"));
        assertTrue(ev.get(1).titulo().contains("Medidor sin señal"));
    }
}
