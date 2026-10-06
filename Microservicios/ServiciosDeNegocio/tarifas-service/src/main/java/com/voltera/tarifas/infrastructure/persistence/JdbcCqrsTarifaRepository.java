package com.voltera.tarifas.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.tarifas.domain.model.Tarifa;
import com.voltera.tarifas.domain.model.TarifaId;
import com.voltera.tarifas.domain.port.out.TarifaRepositoryPort;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador CQRS · Database per Service para Tarifas (diagrama "04 · Patrones").
 * Activo con el perfil {@code cqrs-jdbc}.
 *
 * <p><b>Command</b> ({@code guardar}, {@code eliminar}) escribe en la <b>BD de
 * Escritura</b> y proyecta de inmediato a la <b>BD de Lectura</b> (consistencia de
 * la vista). <b>Query</b> ({@code buscarPorId}, {@code listar}) lee SOLO de la BD de
 * Lectura. El agregado {@link Tarifa} se serializa a JSON (documento) para no acoplar
 * el esquema a su estructura interna.</p>
 */
@Repository
@Profile("cqrs-jdbc")
public class JdbcCqrsTarifaRepository implements TarifaRepositoryPort {

    private final JdbcTemplate write;
    private final JdbcTemplate read;
    private final ObjectMapper mapper;

    public JdbcCqrsTarifaRepository(@Qualifier("writeJdbc") JdbcTemplate write,
                                    @Qualifier("readJdbc") JdbcTemplate read,
                                    ObjectMapper mapper) {
        this.write = write;
        this.read = read;
        this.mapper = mapper;
    }

    @PostConstruct
    void inicializarEsquema() {
        write.execute("CREATE TABLE IF NOT EXISTS tarifa_write (id VARCHAR(128) PRIMARY KEY, doc JSONB NOT NULL, actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now())");
        read.execute("CREATE TABLE IF NOT EXISTS tarifa_read  (id VARCHAR(128) PRIMARY KEY, doc JSONB NOT NULL, proyectado_en TIMESTAMPTZ NOT NULL DEFAULT now())");
    }

    @Override
    public Tarifa guardar(Tarifa tarifa) {
        String id = tarifa.id().valor();
        String doc = aJson(tarifa);
        // Lado COMMAND -> BD Escritura
        write.update("""
            INSERT INTO tarifa_write (id, doc) VALUES (?, ?::jsonb)
            ON CONFLICT (id) DO UPDATE SET doc = EXCLUDED.doc, actualizado_en = now()
            """, id, doc);
        // Proyeccion -> BD Lectura (lado QUERY)
        read.update("""
            INSERT INTO tarifa_read (id, doc) VALUES (?, ?::jsonb)
            ON CONFLICT (id) DO UPDATE SET doc = EXCLUDED.doc, proyectado_en = now()
            """, id, doc);
        return tarifa;
    }

    @Override
    public Optional<Tarifa> buscarPorId(TarifaId id) {
        List<Tarifa> r = read.query("SELECT doc FROM tarifa_read WHERE id = ?",
                (rs, i) -> deJson(rs.getString("doc")), id.valor());
        return r.stream().findFirst();
    }

    @Override
    public List<Tarifa> listar() {
        return read.query("SELECT doc FROM tarifa_read", (rs, i) -> deJson(rs.getString("doc")));
    }

    @Override
    public void eliminar(TarifaId id) {
        write.update("DELETE FROM tarifa_write WHERE id = ?", id.valor());
        read.update("DELETE FROM tarifa_read WHERE id = ?", id.valor());
    }

    private String aJson(Tarifa t) {
        try { return mapper.writeValueAsString(t); }
        catch (Exception e) { throw new IllegalStateException("No se pudo serializar Tarifa", e); }
    }

    private Tarifa deJson(String json) {
        try { return mapper.readValue(json, Tarifa.class); }
        catch (Exception e) { throw new IllegalStateException("No se pudo leer Tarifa de la BD de lectura", e); }
    }
}
