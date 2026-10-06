package com.voltera.ingesta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

/**
 * La autoconfiguracion de DataSource se EXCLUYE del arranque por defecto: el inbox
 * in-memory no requiere base de datos. El DataSource del inbox persistente se crea
 * manualmente solo con el perfil {@code inbox-jdbc} (ver {@code InboxJdbcConfig}).
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
public class IngestaApplication {
    public static void main(String[] args) {
        SpringApplication.run(IngestaApplication.class, args);
    }
}
