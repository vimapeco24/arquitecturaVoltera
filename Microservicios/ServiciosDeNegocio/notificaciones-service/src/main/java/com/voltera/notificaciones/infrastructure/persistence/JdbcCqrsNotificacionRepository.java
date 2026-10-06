package com.voltera.notificaciones.infrastructure.persistence;

import com.voltera.notificaciones.domain.model.CanalNotificacion;
import com.voltera.notificaciones.domain.model.Notificacion;
import com.voltera.notificaciones.domain.model.TipoAlerta;
import com.voltera.notificaciones.domain.port.out.NotificacionRepositoryPort;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Adaptador CQRS · Database per Service para Notificaciones (diagrama "04 · Patrones").
 * Activo con el perfil {@code cqrs-jdbc}. Command (guardar) escribe en la BD de
 * Escritura y proyecta a la BD de Lectura; Query (todas) lee de la BD de Lectura.
 */
@Repository
@Profile("cqrs-jdbc")
public class JdbcCqrsNotificacionRepository implements NotificacionRepositoryPort {

    private static final String COLS = "notificacion_id, medidor_id, tipo, canal, mensaje, generada_en";

    private final JdbcTemplate write;
    private final JdbcTemplate read;

    public JdbcCqrsNotificacionRepository(@Qualifier("writeJdbc") JdbcTemplate write,
                                          @Qualifier("readJdbc") JdbcTemplate read) {
        this.write = write;
        this.read = read;
    }

    @PostConstruct
    void inicializarEsquema() {
        String ddl = "CREATE TABLE IF NOT EXISTS %s (" +
                "notificacion_id VARCHAR(80) PRIMARY KEY, medidor_id VARCHAR(128) NOT NULL, " +
                "tipo VARCHAR(40) NOT NULL, canal VARCHAR(20) NOT NULL, mensaje TEXT, " +
                "generada_en TIMESTAMPTZ NOT NULL)";
        write.execute(String.format(ddl, "notificacion_write"));
        read.execute(String.format(ddl, "notificacion_read"));
    }

    @Override
    public Notificacion guardar(Notificacion n) {
        upsert(write, "notificacion_write", n);  // lado Command -> BD Escritura
        upsert(read, "notificacion_read", n);    // proyeccion   -> BD Lectura
        return n;
    }

    @Override
    public List<Notificacion> todas() {
        return read.query("SELECT " + COLS + " FROM notificacion_read ORDER BY generada_en",
                (rs, i) -> Notificacion.rehidratar(
                        rs.getString("notificacion_id"),
                        rs.getString("medidor_id"),
                        TipoAlerta.valueOf(rs.getString("tipo")),
                        CanalNotificacion.valueOf(rs.getString("canal")),
                        rs.getString("mensaje"),
                        rs.getTimestamp("generada_en").toInstant()));
    }

    private void upsert(JdbcTemplate db, String tabla, Notificacion n) {
        db.update("INSERT INTO " + tabla + " (" + COLS + ") VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (notificacion_id) DO UPDATE SET " +
                        "medidor_id=EXCLUDED.medidor_id, tipo=EXCLUDED.tipo, canal=EXCLUDED.canal, " +
                        "mensaje=EXCLUDED.mensaje, generada_en=EXCLUDED.generada_en",
                n.notificacionId(), n.medidorId(), n.tipo().name(), n.canal().name(),
                n.mensaje(), Timestamp.from(n.generadaEn() != null ? n.generadaEn() : Instant.now()));
    }
}
