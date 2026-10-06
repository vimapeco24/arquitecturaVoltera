package com.voltera.notificaciones.infrastructure.persistence;

import com.voltera.notificaciones.domain.port.out.InboxPort;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
    public boolean registrarSiEsNuevo(String consumidor, String claveIdempotencia, String tipoEvento) {
        int filas = jdbc.update("""
            INSERT INTO inbox_evento (consumidor, clave, tipo_evento)
            VALUES (?, ?, ?)
            ON CONFLICT (consumidor, clave) DO NOTHING
            """, consumidor, claveIdempotencia, tipoEvento);
        if (filas == 0) {
            // Ya existia (reentrega): incrementamos el contador de veces vistas.
            jdbc.update("""
                UPDATE inbox_evento SET veces_vistas = veces_vistas + 1
                WHERE consumidor = ? AND clave = ?
                """, consumidor, claveIdempotencia);
            return false;
        }
        return true;
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
}
