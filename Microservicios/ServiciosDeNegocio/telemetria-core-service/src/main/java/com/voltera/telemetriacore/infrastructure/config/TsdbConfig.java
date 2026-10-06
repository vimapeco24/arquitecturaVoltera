package com.voltera.telemetriacore.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuracion de la base de series temporales (lamina 03 · CQRS: "SerieDeMedicion
 * persiste LecturaValidada append-only en TSDB"). TimescaleDB es una extension de
 * PostgreSQL, por lo que se usa el driver JDBC de PostgreSQL.
 *
 * <p>Se activa SOLO con el perfil {@code tsdb}; la autoconfiguracion de DataSource
 * se excluye en {@code TelemetriaCoreApplication}, de modo que el perfil por defecto
 * no requiere base de datos alguna.</p>
 */
@Configuration
@Profile("tsdb")
public class TsdbConfig {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/voltera_tsdb}") String url,
            @Value("${spring.datasource.username:voltera}") String user,
            @Value("${spring.datasource.password:voltera}") String pass) {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(url)
                .username(user)
                .password(pass)
                .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
