package com.voltera.notificaciones.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * CQRS · Database per Service (diagrama "04 · Diagrama con Patrones").
 *
 * <p>Define DOS datasources PostgreSQL independientes, activos SOLO con el perfil
 * {@code cqrs-jdbc}:</p>
 * <ul>
 *   <li><b>BD Escritura</b> (lado Command): donde el servicio persiste el estado.</li>
 *   <li><b>BD Lectura</b> (lado Query): donde se materializa/consulta la vista.</li>
 * </ul>
 *
 * <p>La conexión es JDBC/SSL (añadir {@code ?sslmode=require} en la URL). La
 * autoconfiguración de DataSource está excluida en la clase Application, de modo que
 * el perfil por defecto (in-memory) no requiere base de datos.</p>
 */
@Configuration
@Profile("cqrs-jdbc")
public class CqrsDataSourceConfig {

    @Bean(name = "writeDataSource")
    public DataSource writeDataSource(
            @Value("${cqrs.write.url:jdbc:postgresql://localhost:5432/voltera_notif_write}") String url,
            @Value("${cqrs.write.username:voltera}") String user,
            @Value("${cqrs.write.password:voltera}") String pass) {
        return DataSourceBuilder.create().type(HikariDataSource.class)
                .driverClassName("org.postgresql.Driver").url(url).username(user).password(pass).build();
    }

    @Bean(name = "readDataSource")
    public DataSource readDataSource(
            @Value("${cqrs.read.url:jdbc:postgresql://localhost:5432/voltera_notif_read}") String url,
            @Value("${cqrs.read.username:voltera}") String user,
            @Value("${cqrs.read.password:voltera}") String pass) {
        return DataSourceBuilder.create().type(HikariDataSource.class)
                .driverClassName("org.postgresql.Driver").url(url).username(user).password(pass).build();
    }

    @Bean(name = "writeJdbc")
    public JdbcTemplate writeJdbc(@Qualifier("writeDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "readJdbc")
    public JdbcTemplate readJdbc(@Qualifier("readDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
