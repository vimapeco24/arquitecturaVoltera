package com.voltera.ia.infrastructure.knowledge;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.port.out.ConocimientoDerPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Caja 2 · Knowledge Service (RAG) para activos DER. Implementación de
 * demostración: devuelve fichas deterministas según el tipo de activo y la
 * anomalía, simulando búsqueda híbrida sobre un índice alimentado por eventos.
 * La interfaz permite sustituirla por pgvector/OpenSearch sin tocar a los agentes.
 */
@Component
public class StubConocimientoDer implements ConocimientoDerPort {

    @Override
    public List<Evidencia> recuperar(AlertaTelemetria a) {
        List<Evidencia> fichas = new ArrayList<>();

        fichas.add(Evidencia.de("RAG:ficha-tecnica",
                "Ficha técnica · " + a.tipoActivo(),
                "Rango nominal del fabricante para '" + a.metrica() + "' y condiciones de operación "
                        + "del " + a.tipoActivo() + " en comunidades energéticas."));

        switch (a.tipoActivo()) {
            case BATERIA -> fichas.add(Evidencia.de("RAG:fallos-conocidos",
                    "Fallos conocidos · batería",
                    "Degradación del State-of-Health (SoH) por ciclos: caídas abruptas de SoH suelen "
                            + "preceder a fallo de celda; procedimiento: inspección térmica en sitio."));
            case INVERSOR -> fichas.add(Evidencia.de("RAG:fallos-conocidos",
                    "Fallos conocidos · inversor",
                    "Inversor fuera de rango de voltaje/frecuencia: riesgo de desconexión y de "
                            + "facturación errónea; procedimiento: reinicio controlado y verificación de red."));
            case PANEL_SOLAR -> fichas.add(Evidencia.de("RAG:fallos-conocidos",
                    "Fallos conocidos · panel solar",
                    "Caída de generación respecto a irradiancia esperada: suciedad, sombreado o "
                            + "string desconectado; procedimiento: limpieza/inspección de strings."));
            case CARGADOR_EV -> fichas.add(Evidencia.de("RAG:fallos-conocidos",
                    "Fallos conocidos · cargador EV",
                    "Errores de sesión de carga intermitentes: firmware o conectividad; "
                            + "procedimiento: actualización de firmware OTA antes de despachar técnico."));
            case MEDIDOR -> fichas.add(Evidencia.de("RAG:fallos-conocidos",
                    "Fallos conocidos · medidor",
                    "Medidor sin señal: pérdida de enlace AMI; procedimiento: verificar gateway de "
                            + "la comunidad antes de asumir fallo del medidor."));
        }

        fichas.add(Evidencia.de("RAG:procedimiento-campo",
                "Procedimiento de campo",
                "Orden de trabajo estándar para '" + a.metrica() + "' en " + a.tipoActivo()
                        + ": checklist de seguridad, repuestos frecuentes y tiempo estimado de atención."));

        return fichas;
    }
}
