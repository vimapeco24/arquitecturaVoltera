package com.voltera.telemetriacore.infrastructure.persistence;

import com.voltera.telemetriacore.domain.model.SerieDeMedicion;
import com.voltera.telemetriacore.domain.port.out.SerieRepositoryPort;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JDBC/TimescaleDB de la SERIE DE MEDICION (lado COMMAND, lamina 03:
 * "SerieDeMedicion persiste LecturaValidada append-only en TSDB"). Activo con el
 * perfil {@code tsdb}.
 *
 * <p>Dos tablas:</p>
 * <ul>
 *   <li>{@code tc_lectura} — append-only de lecturas por medidor (hypertable de
 *       TimescaleDB si la extension esta disponible); es la serie de la lamina.</li>
 *   <li>{@code tc_serie_snapshot} — snapshot del estado del intervalo en curso para
 *       rehidratar el agregado sin reproducir todas las lecturas.</li>
 * </ul>
 *
 * <p>El DDL es idempotente y se ejecuta al iniciar. La creacion de la hypertable es
 * best-effort: si TimescaleDB no esta instalado, la tabla queda como tabla normal.</p>
 */
@Repository
@Profile("tsdb")
public class JdbcSerieRepository implements SerieRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JdbcSerieRepository.class);

    private final JdbcTemplate jdbc;

    public JdbcSerieRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void inicializarEsquema() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tc_lectura (
                medidor_serial VARCHAR(128) NOT NULL,
                consumo_kwh    DOUBLE PRECISION NOT NULL,
                capturada_en   TIMESTAMPTZ NOT NULL
            )
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tc_serie_snapshot (
                medidor_serial          VARCHAR(128) PRIMARY KEY,
                duracion_min            INT NOT NULL,
                intervalo_en_curso_ini  TIMESTAMPTZ,
                acumulado_kwh           DOUBLE PRECISION NOT NULL,
                lecturas_en_intervalo   BIGINT NOT NULL,
                ultima_lectura_en       TIMESTAMPTZ
            )
            """);
        // Hypertable de TimescaleDB (best-effort): si la extension no existe, se ignora.
        try {
            jdbc.execute("SELECT create_hypertable('tc_lectura', 'capturada_en', if_not_exists => TRUE)");
            log.info("TimescaleDB: hypertable tc_lectura lista.");
        } catch (Exception e) {
            log.warn("TimescaleDB no disponible; tc_lectura queda como tabla normal: {}", e.getMessage());
        }
    }

    @Override
    public SerieDeMedicion guardar(SerieDeMedicion serie) {
        // 1) Append-only de la lectura mas reciente (si hay una ultima lectura).
        //    Nota: el agregado acumula; aqui registramos el snapshot del acumulado y,
        //    como traza append-only, insertamos un punto con la ultima lectura observada.
        if (serie.ultimaLecturaEn() != null) {
            jdbc.update("INSERT INTO tc_lectura (medidor_serial, consumo_kwh, capturada_en) VALUES (?, ?, ?)",
                    serie.medidorSerial(), serie.acumuladoKwh(), Timestamp.from(serie.ultimaLecturaEn()));
        }
        // 2) Upsert del snapshot para poder rehidratar el intervalo en curso.
        jdbc.update("""
            INSERT INTO tc_serie_snapshot
                (medidor_serial, duracion_min, intervalo_en_curso_ini, acumulado_kwh, lecturas_en_intervalo, ultima_lectura_en)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (medidor_serial) DO UPDATE SET
                duracion_min = EXCLUDED.duracion_min,
                intervalo_en_curso_ini = EXCLUDED.intervalo_en_curso_ini,
                acumulado_kwh = EXCLUDED.acumulado_kwh,
                lecturas_en_intervalo = EXCLUDED.lecturas_en_intervalo,
                ultima_lectura_en = EXCLUDED.ultima_lectura_en
            """,
                serie.medidorSerial(),
                serie.duracionMin(),
                serie.intervaloEnCursoInicio() != null ? Timestamp.from(serie.intervaloEnCursoInicio()) : null,
                serie.acumuladoKwh(),
                serie.lecturasEnIntervalo(),
                serie.ultimaLecturaEn() != null ? Timestamp.from(serie.ultimaLecturaEn()) : null);
        return serie;
    }

    @Override
    public Optional<SerieDeMedicion> buscarPorMedidor(String medidorSerial) {
        List<SerieDeMedicion> r = jdbc.query(
                "SELECT * FROM tc_serie_snapshot WHERE medidor_serial = ?",
                (rs, i) -> rehidratar(rs.getString("medidor_serial"),
                        rs.getInt("duracion_min"),
                        instante(rs.getTimestamp("intervalo_en_curso_ini")),
                        rs.getDouble("acumulado_kwh"),
                        rs.getLong("lecturas_en_intervalo"),
                        instante(rs.getTimestamp("ultima_lectura_en"))),
                medidorSerial);
        return r.stream().findFirst();
    }

    @Override
    public List<SerieDeMedicion> todas() {
        return jdbc.query("SELECT * FROM tc_serie_snapshot",
                (rs, i) -> rehidratar(rs.getString("medidor_serial"),
                        rs.getInt("duracion_min"),
                        instante(rs.getTimestamp("intervalo_en_curso_ini")),
                        rs.getDouble("acumulado_kwh"),
                        rs.getLong("lecturas_en_intervalo"),
                        instante(rs.getTimestamp("ultima_lectura_en"))));
    }

    private SerieDeMedicion rehidratar(String serial, int duracionMin, Instant intervaloIni,
                                       double acumulado, long lecturas, Instant ultima) {
        return SerieDeMedicion.rehidratar(serial, duracionMin, intervaloIni, acumulado, lecturas, ultima);
    }

    private Instant instante(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
