package com.voltera.ia.application;

import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.gobernanza.AuditoriaPropuesta;
import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.MetricasGobernanza;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Auditoría por propuesta y métricas agregadas de la gobernanza 3/3. */
class AuditoriaMetricasGobernanzaTest {

    private PropuestaAccion nueva() {
        return PropuestaAccion.crear(UUID.randomUUID(), "INV-1", "COM", TipoAccion.COMANDO_FISICO,
                "REINICIAR", "", Severidad.CRITICA, "hash", "diagnosticador@v3", "local", Duration.ofMinutes(30));
    }

    private PropuestaAccion ejecutada() {
        PropuestaAccion p = nueva();
        p.firmar(Firma.aprobacion(Firmante.OPERADOR, "op", "ok"));
        p.firmar(Firma.aprobacion(Firmante.PROSUMIDOR, "pro", "ok"));
        p.marcarEjecutada("OK");
        return p;
    }

    @Test
    void auditoriaDeUnaPropuestaEjecutadaListaTodosLosHitosEnOrden() {
        AuditoriaPropuesta a = AuditoriaPropuesta.desde(ejecutada());
        List<String> tipos = a.eventos().stream().map(AuditoriaPropuesta.Evento::tipo).toList();
        assertEquals("PROPUESTA_CREADA", tipos.get(0));
        assertTrue(tipos.containsAll(List.of("FIRMA_AGENTE_IA", "FIRMA_OPERADOR", "FIRMA_PROSUMIDOR", "APROBADA_3_DE_3", "EJECUTADA")));
        assertEquals("EJECUTADA", a.estado());
        assertEquals("hash", a.hashDiagnostico());
    }

    @Test
    void auditoriaDeRechazoRegistraElRechazoYNoLaEjecucion() {
        PropuestaAccion p = nueva();
        p.firmar(Firma.rechazo(Firmante.OPERADOR, "op", "no procede"));
        List<String> tipos = AuditoriaPropuesta.desde(p).eventos().stream().map(AuditoriaPropuesta.Evento::tipo).toList();
        assertTrue(tipos.contains("RECHAZO_OPERADOR"));
        assertTrue(!tipos.contains("EJECUTADA"));
    }

    @Test
    void metricasContabilizanEstadosYGarantizanCeroEjecucionesSin3de3() {
        PropuestaAccion rechazada = nueva();
        rechazada.firmar(Firma.rechazo(Firmante.PROSUMIDOR, "pro", "no autorizo"));
        MetricasGobernanza m = MetricasGobernanza.desde(List.of(ejecutada(), rechazada, nueva()));
        assertEquals(3, m.total());
        assertEquals(1, m.porEstado().get("EJECUTADA"));
        assertEquals(1, m.porEstado().get("RECHAZADA"));
        assertEquals(1, m.porEstado().get("PENDIENTE"));
        assertEquals(0, m.ejecutadasSin3de3());
        assertEquals(0.33, m.tasaEjecucion());
    }

    @Test
    void metricasSinPropuestasNoFallan() {
        MetricasGobernanza m = MetricasGobernanza.desde(List.of());
        assertEquals(0, m.total());
        assertEquals(0.0, m.tasaEjecucion());
    }
}
