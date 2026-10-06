package com.voltera.telemetriacore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

/**
 * Las autoconfiguraciones de DataSource y Redis se EXCLUYEN del arranque por
 * defecto: el perfil default usa adaptadores in-memory y no requiere ninguna
 * conexion externa. Los beans de infraestructura (DataSource JDBC / Redis) se
 * crean manualmente solo cuando se activa el perfil correspondiente
 * ({@code tsdb} o {@code redis}), evitando que el servicio falle al arrancar sin
 * esas bases de datos.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
public class TelemetriaCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelemetriaCoreApplication.class, args);
    }
}
