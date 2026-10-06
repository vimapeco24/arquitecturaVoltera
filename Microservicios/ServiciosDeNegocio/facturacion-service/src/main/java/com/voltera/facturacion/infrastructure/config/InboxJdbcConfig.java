package com.voltera.facturacion.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * DataSource/JdbcTemplate para el INBOX persistente (lamina 10), activo SOLO con el
 * perfil {@code inbox-jdbc}. La autoconfiguracion de DataSource se excluye en la
 * clase Application, de modo que el perfil por defecto no requiere base de datos.
 * TimescaleDB/PostgreSQL comparten el driver JDBC de PostgreSQL.
 */
@Configuration
@Profile("inbox-jdbc")
public class InboxJdbcConfig {

    @Bean
    public DataSource inboxDataSource(
            @Value("${inbox.datasource.url:jdbc:postgresql://localhost:5432/voltera_inbox}") String url,
            @Value("${inbox.datasource.username:voltera}") String user,
            @Value("${inbox.datasource.password:voltera}") String pass) {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(url)
                .username(user)
                .password(pass)
                .build();
    }

    @Bean
    public JdbcTemplate inboxJdbcTemplate(DataSource inboxDataSource) {
        return new JdbcTemplate(inboxDataSource);
    }
}
