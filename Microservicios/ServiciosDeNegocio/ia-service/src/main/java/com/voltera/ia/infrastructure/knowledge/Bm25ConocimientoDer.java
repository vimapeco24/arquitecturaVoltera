package com.voltera.ia.infrastructure.knowledge;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.port.out.ConocimientoDerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Caja 2 · Knowledge Service (RAG) real: recuperación léxica BM25 sobre el corpus
 * de documentos de {@code classpath:conocimiento/*.md} (fichas técnicas, fallos
 * conocidos, procedimientos de campo y regulación).
 *
 * <p>Cada documento se divide en fragmentos por sección ({@code ## }). La consulta se
 * arma con la alerta (tipo de activo, métrica, descripción y síntoma) y se filtra por
 * el tipo de activo. Para el diagnóstico se devuelve el mejor fragmento de cada
 * categoría, con su puntaje, para que el agente siempre cite ficha, fallo y
 * procedimiento. Es recuperación léxica, no vectorial: suficiente para un corpus
 * pequeño y totalmente explicable.</p>
 */
@Component
public class Bm25ConocimientoDer implements ConocimientoDerPort {

    private static final Logger log = LoggerFactory.getLogger(Bm25ConocimientoDer.class);
    private static final double K1 = 1.2;
    private static final double B = 0.75;
    static final List<String> CATEGORIAS = List.of("ficha-tecnica", "fallos-conocidos", "procedimiento-campo");
    private static final Set<String> STOP = Set.of(
            "de", "la", "el", "en", "y", "a", "los", "las", "del", "se", "por", "un", "una", "con", "para",
            "que", "es", "al", "o", "su", "sus", "no", "si", "lo", "como", "mas", "entre", "sin", "este",
            "esta", "cada", "debe", "hasta", "sobre", "antes", "tras", "le", "ya");

    /** Fragmento indexado (una sección de un documento). */
    public record Fragmento(String docId, String titulo, String categoria, List<String> activos,
                            String fuente, String seccion, String texto, Map<String, Integer> tf, int largo) {}

    /** Resultado de búsqueda con su puntaje BM25 y los términos que coincidieron. */
    public record Resultado(String docId, String titulo, String categoria, String seccion, String texto,
                            String fuente, double score, List<String> terminos) {}

    /** Documento del corpus, para el catálogo. */
    public record Documento(String id, String titulo, String categoria, List<String> activos,
                            String fuente, int secciones) {}

    private final List<Fragmento> fragmentos = new ArrayList<>();
    private final List<Documento> documentos = new ArrayList<>();
    private final Map<String, Integer> df = new HashMap<>();
    private double largoPromedio = 1;

    public Bm25ConocimientoDer() {
        this("classpath:conocimiento/*.md");
    }

    Bm25ConocimientoDer(String patron) {
        try {
            Resource[] rs = new PathMatchingResourcePatternResolver().getResources(patron);
            for (Resource r : rs) {
                try (InputStream in = r.getInputStream()) {
                    indexar(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            log.warn("[rag] no se pudo cargar el corpus {}: {}", patron, e.getMessage());
        }
        largoPromedio = fragmentos.stream().mapToInt(Fragmento::largo).average().orElse(1);
        log.info("[rag] corpus BM25: {} documentos, {} fragmentos, {} términos",
                documentos.size(), fragmentos.size(), df.size());
    }

    private void indexar(String contenido) {
        String[] partes = contenido.split("\\R---\\R", 2);
        if (partes.length < 2) return;
        Map<String, String> meta = new LinkedHashMap<>();
        for (String linea : partes[0].split("\\R")) {
            int i = linea.indexOf(':');
            if (i > 0) meta.put(linea.substring(0, i).trim(), linea.substring(i + 1).trim());
        }
        String id = meta.getOrDefault("id", "doc-" + documentos.size());
        String titulo = meta.getOrDefault("titulo", id);
        String categoria = meta.getOrDefault("categoria", "general");
        List<String> activos = Arrays.stream(meta.getOrDefault("activos", "GENERAL").split(","))
                .map(String::trim).toList();
        String fuente = meta.getOrDefault("fuente", "");
        int n = 0;
        for (String bloque : partes[1].split("(?m)^## ")) {
            if (bloque.isBlank()) continue;
            String[] lineas = bloque.split("\\R", 2);
            String seccion = lineas[0].trim();
            String texto = lineas.length > 1 ? lineas[1].trim().replaceAll("\\s+", " ") : "";
            List<String> tokens = tokenizar(titulo + " " + seccion + " " + texto);
            Map<String, Integer> tf = new HashMap<>();
            tokens.forEach(t -> tf.merge(t, 1, Integer::sum));
            tf.keySet().forEach(t -> df.merge(t, 1, Integer::sum));
            fragmentos.add(new Fragmento(id, titulo, categoria, activos, fuente, seccion, texto, tf, tokens.size()));
            n++;
        }
        documentos.add(new Documento(id, titulo, categoria, activos, fuente, n));
    }

    static List<String> tokenizar(String s) {
        String sinTildes = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String t : sinTildes.split("[^a-z0-9]+")) {
            if (t.length() < 2 || STOP.contains(t)) continue;
            out.add(t.length() > 5 ? t.substring(0, t.length() - (t.endsWith("es") ? 2 : t.endsWith("s") ? 1 : 0)) : t);
        }
        return out;
    }

    /** Búsqueda BM25 sobre el corpus, opcionalmente filtrada por tipo de activo y categoría. */
    public List<Resultado> buscar(String consulta, String tipoActivo, String categoria, int k) {
        List<String> q = tokenizar(consulta).stream().distinct().toList();
        int N = fragmentos.size();
        List<Resultado> res = new ArrayList<>();
        for (Fragmento f : fragmentos) {
            if (tipoActivo != null && !tipoActivo.isBlank()
                    && !f.activos().contains(tipoActivo) && !f.activos().contains("GENERAL")) continue;
            if (categoria != null && !categoria.isBlank() && !f.categoria().equals(categoria)) continue;
            double score = 0;
            List<String> hits = new ArrayList<>();
            for (String t : q) {
                Integer tf = f.tf().get(t);
                if (tf == null) continue;
                int dft = df.getOrDefault(t, 0);
                double idf = Math.log(1 + (N - dft + 0.5) / (dft + 0.5));
                score += idf * (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * f.largo() / largoPromedio));
                hits.add(t);
            }
            if (score > 0) {
                res.add(new Resultado(f.docId(), f.titulo(), f.categoria(), f.seccion(), f.texto(),
                        f.fuente(), Math.round(score * 100.0) / 100.0, hits));
            }
        }
        res.sort(Comparator.comparingDouble(Resultado::score).reversed());
        return res.subList(0, Math.min(k, res.size()));
    }

    /** Consulta que se arma a partir de la alerta (lo que el agente "pregunta" al RAG). */
    public static String consultaDesde(AlertaTelemetria a) {
        StringBuilder q = new StringBuilder();
        q.append(a.tipoActivo()).append(' ').append(a.metrica()).append(' ').append(a.descripcion());
        if (a.sinSenal()) q.append(" sin señal enlace concentrador gateway lecturas");
        else if (a.fueraDeRango()) q.append(" fuera de rango sostenida riesgo desconexión umbral");
        else q.append(" dentro de umbrales monitoreo reforzado leve");
        if ("voltaje".equalsIgnoreCase(a.metrica())) q.append(" tensión sobretensión red");
        return q.toString();
    }

    @Override
    public List<Evidencia> recuperar(AlertaTelemetria a) {
        String q = consultaDesde(a);
        String tipo = a.tipoActivo() == null ? null : a.tipoActivo().name();
        List<Evidencia> out = new ArrayList<>();
        for (String cat : CATEGORIAS) {
            List<Resultado> r = buscar(q, tipo, cat, 1);
            if (r.isEmpty()) continue;
            Resultado m = r.get(0);
            out.add(Evidencia.de("RAG:" + cat,
                    m.titulo() + " · §" + m.seccion() + " (BM25 " + m.score() + ")",
                    m.texto() + " [doc " + m.docId() + "]"));
        }
        return out;
    }

    public List<Documento> documentos() {
        return List.copyOf(documentos);
    }

    public int totalFragmentos() {
        return fragmentos.size();
    }
}
