package com.voltera.ia.infrastructure.knowledge;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.SolicitudCopiloto;
import com.voltera.ia.domain.port.out.ConocimientoPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Knowledge Service (RAG) de demostracion. En la arquitectura de referencia este
 * componente hace busqueda hibrida (vectores + palabras clave) sobre un indice
 * alimentado por los eventos del negocio, devolviendo fragmentos con su fuente.
 *
 * <p>Aqui se simula con fichas deterministas derivadas del contexto del caso,
 * de modo que la demo cite fuentes coherentes sin un indice vectorial real. La
 * interfaz {@link ConocimientoPort} permite sustituirlo por pgvector/OpenSearch
 * sin tocar al agente.</p>
 */
@Component
public class StubKnowledgeService implements ConocimientoPort {

    @Override
    public List<Evidencia> recuperar(SolicitudCopiloto s) {
        List<Evidencia> fichas = new ArrayList<>();

        fichas.add(Evidencia.de("RAG:polizas",
                "Cobertura de la poliza " + nz(s.polizaId()),
                "La poliza cubre daños a equipos de generacion distribuida (paneles, "
                        + "inversores) por sobretension y eventos climaticos, con deducible del 10%."));

        fichas.add(Evidencia.de("RAG:normativa",
                "Normativa de reclamaciones de energia P2P",
                "Resolucion marco: toda reclamacion sobre activos de la comunidad energetica "
                        + "requiere peritaje tecnico y verificacion de telemetria del medidor asociado."));

        BigDecimal monto = s.montoReclamacion();
        if (monto.compareTo(new BigDecimal("3000000")) > 0) {
            fichas.add(Evidencia.de("RAG:historial",
                    "Historial de reclamaciones del prosumidor " + nz(s.prosumidorId()),
                    "Reclamaciones de alto monto en este segmento tienen tasa de fraude superior "
                            + "al promedio; se recomienda inspeccion fisica antes de aprobar."));
        } else {
            fichas.add(Evidencia.de("RAG:historial",
                    "Historial de reclamaciones del prosumidor " + nz(s.prosumidorId()),
                    "Sin reclamaciones previas relevantes; patron consistente con un evento puntual."));
        }

        return fichas;
    }

    private static String nz(String v) {
        return (v == null || v.isBlank()) ? "(sin-id)" : v;
    }
}
