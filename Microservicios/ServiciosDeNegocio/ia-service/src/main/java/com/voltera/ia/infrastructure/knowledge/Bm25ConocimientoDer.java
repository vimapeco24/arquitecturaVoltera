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
 * <p>Cada documento se divide en fragmentos por sección ({@code ## }). La búsqueda es
 * híbrida: BM25 (palabras clave) y similitud coseno sobre vectores TF-IDF, fusionadas
 * con Reciprocal Rank Fusion (k = 60) y reordenadas por cobertura de los términos de la
 * consulta. Respeta permisos por documento ({@code acceso: PUBLICO | OPERADOR}). El
 * índice es una proyección: se ingieren y retiran documentos en caliente.</p>
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
                            String fuente, String seccion, String texto, Map<String, Integer> tf, int largo,
                            String acceso) {}

    /**
     * Resultado de búsqueda. {@code score} es el puntaje híbrido final (0..1 relativo al mejor);
     * {@code bm25} y {@code coseno} son los puntajes de cada recuperador.
     */
    public record Resultado(String docId, String titulo, String categoria, String seccion, String texto,
                            String fuente, double score, List<String> terminos, double bm25, double coseno,
                            double cobertura, String acceso) {}

    /** Documento del corpus, para el catálogo. */
    public record Documento(String id, String titulo, String categoria, List<String> activos,
                            String fuente, int secciones, String acceso, String origen) {}

    private final List<Fragmento> fragmentos = new ArrayList<>();
    private final List<Documento> documentos = new ArrayList<>();
    private final Map<String, Integer> df = new HashMap<>();
    private double largoPromedio = 1;
    private static final int RRF_K = 60;

    public Bm25ConocimientoDer() {
        this("classpath:conocimiento/*.md");
    }

    Bm25ConocimientoDer(String patron) {
        try {
            Resource[] rs = new PathMatchingResourcePatternResolver().getResources(patron);
            for (Resource r : rs) {
                try (InputStream in = r.getInputStream()) {
                    indexar(new String(in.readAllBytes(), StandardCharsets.UTF_8), "CORPUS");
                }
            }
        } catch (IOException e) {
            log.warn("[rag] no se pudo cargar el corpus {}: {}", patron, e.getMessage());
        }
        recalcular();
        log.info("[rag] corpus BM25: {} documentos, {} fragmentos, {} términos",
                documentos.size(), fragmentos.size(), df.size());
    }

    private void recalcular() {
        df.clear();
        for (Fragmento f : fragmentos) f.tf().keySet().forEach(t -> df.merge(t, 1, Integer::sum));
        largoPromedio = fragmentos.stream().mapToInt(Fragmento::largo).average().orElse(1);
    }

    /** Ingesta (proyección alimentada por eventos o por la consola): agrega o reemplaza un documento. */
    public synchronized Documento ingerir(String contenido) {
        if (contenido == null || contenido.length() > 20_000) throw new IllegalArgumentException("Documento vacío o mayor a 20 000 caracteres.");
        String id = indexar(contenido, "INGESTA");
        if (id == null) throw new IllegalArgumentException("Formato inválido: encabezado 'id:/titulo:/categoria:/activos:' seguido de '---' y secciones '## '.");
        recalcular();
        return documentos.stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }

    /** Supresión propagada: retira el documento del índice. */
    public synchronized boolean retirar(String id) {
        boolean quitado = documentos.removeIf(d -> d.id().equals(id));
        fragmentos.removeIf(f -> f.docId().equals(id));
        recalcular();
        return quitado;
    }

    private String indexar(String contenido, String origen) {
        String[] partes = contenido.split("\\R---\\R", 2);
        if (partes.length < 2) return null;
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
        String acceso = meta.getOrDefault("acceso", "PUBLICO").toUpperCase(Locale.ROOT);
        if (!Set.of("PUBLICO", "OPERADOR").contains(acceso)) acceso = "OPERADOR";
        documentos.removeIf(d -> d.id().equals(id));
        fragmentos.removeIf(f -> f.docId().equals(id));
        int n = 0;
        for (String bloque : partes[1].split("(?m)^## ")) {
            if (bloque.isBlank()) continue;
            String[] lineas = bloque.split("\\R", 2);
            String seccion = lineas[0].trim();
            String texto = lineas.length > 1 ? lineas[1].trim().replaceAll("\\s+", " ") : "";
            List<String> tokens = tokenizar(titulo + " " + seccion + " " + texto);
            Map<String, Integer> tf = new HashMap<>();
            tokens.forEach(t -> tf.merge(t, 1, Integer::sum));
            fragmentos.add(new Fragmento(id, titulo, categoria, activos, fuente, seccion, texto, tf, tokens.size(), acceso));
            n++;
        }
        if (n == 0) return null;
        documentos.add(new Documento(id, titulo, categoria, activos, fuente, n, acceso, origen));
        return id;
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

    /** Búsqueda híbrida con permisos de operador (uso interno del agente). */
    public List<Resultado> buscar(String consulta, String tipoActivo, String categoria, int k) {
        return buscar(consulta, tipoActivo, categoria, k, "OPERADOR");
    }

    /**
     * Búsqueda híbrida: BM25 + coseno TF-IDF, fusión RRF y reordenamiento por cobertura.
     * {@code rol} = PROSUMIDOR solo ve documentos PUBLICO; OPERADOR ve todos.
     */
    public synchronized List<Resultado> buscar(String consulta, String tipoActivo, String categoria, int k, String rol) {
        List<String> q = tokenizar(consulta).stream().distinct().toList();
        if (q.isEmpty()) return List.of();
        int N = fragmentos.size();
        boolean soloPublico = rol != null && rol.equalsIgnoreCase("PROSUMIDOR");
        record Cand(Fragmento f, double bm25, double cos, List<String> hits) {}
        List<Cand> cands = new ArrayList<>();
        Map<String, Double> idf = new HashMap<>();
        for (String t : q) {
            int dft = df.getOrDefault(t, 0);
            idf.put(t, Math.log(1 + (N - dft + 0.5) / (dft + 0.5)));
        }
        double qNorm = Math.sqrt(q.stream().mapToDouble(t -> idf.get(t) * idf.get(t)).sum());
        for (Fragmento f : fragmentos) {
            if (soloPublico && !"PUBLICO".equals(f.acceso())) continue;
            if (tipoActivo != null && !tipoActivo.isBlank()
                    && !f.activos().contains(tipoActivo) && !f.activos().contains("GENERAL")) continue;
            if (categoria != null && !categoria.isBlank() && !f.categoria().equals(categoria)) continue;
            double bm = 0, dot = 0;
            List<String> hits = new ArrayList<>();
            for (String t : q) {
                Integer tf = f.tf().get(t);
                if (tf == null) continue;
                double w = idf.get(t);
                bm += w * (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * f.largo() / largoPromedio));
                dot += (tf * w) * w;
                hits.add(t);
            }
            if (hits.isEmpty()) continue;
            double dNorm = Math.sqrt(f.tf().entrySet().stream().mapToDouble(e -> {
                int dft = df.getOrDefault(e.getKey(), 1);
                double w = e.getValue() * Math.log(1 + (N - dft + 0.5) / (dft + 0.5));
                return w * w;
            }).sum());
            cands.add(new Cand(f, bm, dNorm == 0 || qNorm == 0 ? 0 : dot / (dNorm * qNorm), hits));
        }
        List<Cand> porBm = cands.stream().sorted(Comparator.comparingDouble(Cand::bm25).reversed()).toList();
        List<Cand> porCos = cands.stream().sorted(Comparator.comparingDouble(Cand::cos).reversed()).toList();
        List<Resultado> res = new ArrayList<>();
        for (Cand c : cands) {
            double rrf = 1.0 / (RRF_K + porBm.indexOf(c) + 1) + 1.0 / (RRF_K + porCos.indexOf(c) + 1);
            double cobertura = (double) c.hits().size() / q.size();
            double finalScore = rrf * (0.5 + 0.5 * cobertura);
            res.add(new Resultado(c.f().docId(), c.f().titulo(), c.f().categoria(), c.f().seccion(), c.f().texto(),
                    c.f().fuente(), finalScore, c.hits(), r2(c.bm25()), r3(c.cos()), r2(cobertura), c.f().acceso()));
        }
        res.sort(Comparator.comparingDouble(Resultado::score).reversed());
        double max = res.isEmpty() ? 1 : res.get(0).score();
        return res.stream().limit(k).map(r -> new Resultado(r.docId(), r.titulo(), r.categoria(), r.seccion(), r.texto(),
                r.fuente(), r3(r.score() / max), r.terminos(), r.bm25(), r.coseno(), r.cobertura(), r.acceso())).toList();
    }

    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double r3(double v) { return Math.round(v * 1000.0) / 1000.0; }

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
                    m.titulo() + " · §" + m.seccion() + " (BM25 " + m.bm25() + " · coseno " + m.coseno() + ")",
                    m.texto() + " [doc " + m.docId() + "]"));
        }
        return out;
    }

    public synchronized List<Documento> documentos() {
        return List.copyOf(documentos);
    }

    public synchronized int totalFragmentos() {
        return fragmentos.size();
    }
}
