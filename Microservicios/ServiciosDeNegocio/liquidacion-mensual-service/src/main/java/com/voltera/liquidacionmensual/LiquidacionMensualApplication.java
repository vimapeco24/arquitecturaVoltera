package com.voltera.liquidacionmensual;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

/**
 * Punto de arranque del microservicio de Liquidacion Mensual.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
public class LiquidacionMensualApplication {
    public static void main(String[] args) {
        SpringApplication.run(LiquidacionMensualApplication.class, args);
    }
}
