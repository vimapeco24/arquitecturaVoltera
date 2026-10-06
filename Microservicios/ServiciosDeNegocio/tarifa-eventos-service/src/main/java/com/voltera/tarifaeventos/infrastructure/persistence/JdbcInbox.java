package com.voltera.tarifaeventos.infrastructure.persistence;

import com.voltera.tarifaeventos.domain.model.MensajeInbox;
import com.voltera.tarifaeventos.domain.port.out.InboxPort;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Adaptador JDBC de la tabla INBOX de idempotencia (lamina 10), activo con el perfil
 * {@code inbox-jdbc}. La clave primaria ({@code consumidor}, {@code clave}) hace que
 * una reentrega del mismo evento no vuelva a procesarse: el {@code INSERT ... ON
 * CONFLICT DO NOTHING} devuelve 0 filas afectadas cuando ya existia.
 */
@Repository
@Profile("inbox-jdbc")
public class JdbcInbox implements InboxPort {

    private final JdbcTemplate jdbc;

    public JdbcInbox(JdbcTemplate inboxJdbcTemplate) {
        this.jdbc = inboxJdbcTemplate;
    }

    @PostConstruct
    void inicializarEsquema() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS inbox_evento (
                consumidor   VARCHAR(160) NOT NULL,
                clave        VARCHAR(256) NOT NULL,
                tipo_evento  VARCHAR(120) NOT NULL,
                primera_vez  TIMESTAMPTZ NOT NULL DEFAULT now(),
                veces_vistas BIGINT NOT NULL DEFAULT 1,
                PRIMARY KEY (consumidor, clave)
            )
            """);
    }

    @Override
    public boolean registrarSiEsNuevo(String consumidor, String eventId, String tipoEvento) {
        int filas = jdbc.update("""
            INSERT INTO inbox_evento (consumidor, clave, tipo_evento)
            VALUES (?, ?, ?)
            ON CONFLICT (consumidor, clave) DO NOTHING
            """, consumidor, eventId, tipoEvento);
        if (filas == 0) {
            jdbc.update("""
                UPDATE inbox_evento SET veces_vistas = veces_vistas + 1
                WHERE consumidor = ? AND clave = ?
                """, consumidor, eventId);
            return false;
        }
        return true;
    }

    @Override
    public List<MensajeInbox> todos() {
        return jdbc.query("SELECT consumidor, clave, tipo_evento, primera_vez, veces_vistas FROM inbox_evento",
                (rs, i) -> new MensajeInbox(
                        rs.getString("consumidor"),
                        rs.getString("clave"),
                        rs.getString("tipo_evento"),
                        instante(rs.getTimestamp("primera_vez")),
                        rs.getLong("veces_vistas")));
    }

    @Override
    public long totalUnicos() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM inbox_evento", Long.class);
        return n != null ? n : 0L;
    }

    @Override
    public long totalDuplicados() {
        Long n = jdbc.queryForObject(
                "SELECT COALESCE(SUM(veces_vistas - 1), 0) FROM inbox_evento", Long.class);
        return n != null ? n : 0L;
    }

    private Instant instante(Timestamp ts) {
        return ts != null ? ts.toInstant() : Instant.now();
    }
}
