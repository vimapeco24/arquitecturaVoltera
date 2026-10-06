package com.voltera.habilitacion.infrastructure.persistence;

import com.voltera.habilitacion.domain.model.DatosComerciales;
import com.voltera.habilitacion.domain.model.EstadoHabilitacion;
import com.voltera.habilitacion.domain.model.IdentidadDispositivo;
import com.voltera.habilitacion.domain.model.Medidor;
import com.voltera.habilitacion.domain.model.PuntoDeMedicion;
import com.voltera.habilitacion.domain.port.out.MedidorRepositoryPort;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador CQRS · Database per Service para Habilitacion = "Serv. de Transferencia
 * de Estado" (diagrama "04 · Patrones"). Activo con el perfil {@code cqrs-jdbc}.
 *
 * <p>El estado del medidor (ECST) se persiste en la <b>BD de Escritura</b> (lado
 * Command) y se proyecta a la <b>BD de Lectura</b> (lado Query), que sirve las
 * consultas {@code buscarPorId/buscarPorSerial/pendientes/todos}.</p>
 */
@Repository
@Profile("cqrs-jdbc")
public class JdbcCqrsMedidorRepository implements MedidorRepositoryPort {

    private static final String COLS =
            "medidor_id, serial, fabricante, codigo_punto, direccion, cliente_id, proveedor_ami, " +
            "protocolo, plan, canales, orden_instalacion_id, habilitado_en, estado, canal_ingesta_creado, " +
            "tarifa_asignada, finalizado_en, motivo_fallo";

    private final JdbcTemplate write;
    private final JdbcTemplate read;

    public JdbcCqrsMedidorRepository(@Qualifier("writeJdbc") JdbcTemplate write,
                                     @Qualifier("readJdbc") JdbcTemplate read) {
        this.write = write;
        this.read = read;
    }

    @PostConstruct
    void inicializarEsquema() {
        String ddl = "CREATE TABLE IF NOT EXISTS %s (" +
                "medidor_id VARCHAR(128) PRIMARY KEY, serial VARCHAR(128), fabricante VARCHAR(128), " +
                "codigo_punto VARCHAR(128), direccion TEXT, cliente_id VARCHAR(128), proveedor_ami VARCHAR(128), " +
                "protocolo VARCHAR(40), plan VARCHAR(80), canales TEXT, orden_instalacion_id VARCHAR(128), " +
                "habilitado_en TIMESTAMPTZ, estado VARCHAR(20) NOT NULL, canal_ingesta_creado BOOLEAN NOT NULL, " +
                "tarifa_asignada BOOLEAN NOT NULL, finalizado_en TIMESTAMPTZ, motivo_fallo TEXT)";
        write.execute(String.format(ddl, "medidor_write"));
        read.execute(String.format(ddl, "medidor_read"));
    }

    @Override
    public Medidor guardar(Medidor m) {
        upsert(write, "medidor_write", m);  // Command -> BD Escritura
        upsert(read, "medidor_read", m);    // proyeccion -> BD Lectura
        return m;
    }

    @Override
    public Optional<Medidor> buscarPorId(String medidorId) {
        return read.query("SELECT " + COLS + " FROM medidor_read WHERE medidor_id = ?", mapper(), medidorId)
                .stream().findFirst();
    }

    @Override
    public Optional<Medidor> buscarPorSerial(String serial) {
        if (serial == null) return Optional.empty();
        return read.query("SELECT " + COLS + " FROM medidor_read WHERE serial = ?", mapper(), serial)
                .stream().findFirst();
    }

    @Override
    public List<Medidor> pendientes() {
        return read.query("SELECT " + COLS + " FROM medidor_read WHERE estado = 'PENDIENTE'", mapper());
    }

    @Override
    public List<Medidor> todos() {
        return read.query("SELECT " + COLS + " FROM medidor_read", mapper());
    }

    private void upsert(JdbcTemplate db, String tabla, Medidor m) {
        DatosComerciales dc = m.datosComerciales();
        db.update("INSERT INTO " + tabla + " (" + COLS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                        "ON CONFLICT (medidor_id) DO UPDATE SET " +
                        "serial=EXCLUDED.serial, fabricante=EXCLUDED.fabricante, codigo_punto=EXCLUDED.codigo_punto, " +
                        "direccion=EXCLUDED.direccion, cliente_id=EXCLUDED.cliente_id, proveedor_ami=EXCLUDED.proveedor_ami, " +
                        "protocolo=EXCLUDED.protocolo, plan=EXCLUDED.plan, canales=EXCLUDED.canales, " +
                        "orden_instalacion_id=EXCLUDED.orden_instalacion_id, habilitado_en=EXCLUDED.habilitado_en, " +
                        "estado=EXCLUDED.estado, canal_ingesta_creado=EXCLUDED.canal_ingesta_creado, " +
                        "tarifa_asignada=EXCLUDED.tarifa_asignada, finalizado_en=EXCLUDED.finalizado_en, " +
                        "motivo_fallo=EXCLUDED.motivo_fallo",
                m.medidorId(), m.identidad().serial(), m.identidad().fabricante(),
                m.punto().codigoPunto(), m.punto().direccion(),
                dc.clienteId(), dc.proveedorAmi(), dc.protocolo(), dc.plan(), dc.canalesComoTexto(),
                m.ordenInstalacionId(),
                m.habilitadoEn() != null ? Timestamp.from(m.habilitadoEn()) : null,
                m.estado().name(), m.canalIngestaCreado(), m.tarifaAsignada(),
                m.finalizadoEn() != null ? Timestamp.from(m.finalizadoEn()) : null,
                m.motivoFallo());
    }

    private RowMapper<Medidor> mapper() {
        return (rs, i) -> {
            String canales = rs.getString("canales");
            DatosComerciales dc = new DatosComerciales(
                    rs.getString("cliente_id"), rs.getString("proveedor_ami"),
                    rs.getString("protocolo"), rs.getString("plan"),
                    (canales == null || canales.isBlank()) ? null : List.of(canales.split(",")));
            Timestamp hab = rs.getTimestamp("habilitado_en");
            Timestamp fin = rs.getTimestamp("finalizado_en");
            return Medidor.rehidratar(
                    rs.getString("medidor_id"),
                    new IdentidadDispositivo(rs.getString("serial"), rs.getString("fabricante")),
                    new PuntoDeMedicion(rs.getString("codigo_punto"), rs.getString("direccion")),
                    dc,
                    rs.getString("orden_instalacion_id"),
                    hab != null ? hab.toInstant() : Instant.now(),
                    EstadoHabilitacion.valueOf(rs.getString("estado")),
                    rs.getBoolean("canal_ingesta_creado"),
                    rs.getBoolean("tarifa_asignada"),
                    fin != null ? fin.toInstant() : null,
                    rs.getString("motivo_fallo"));
        };
    }
}
