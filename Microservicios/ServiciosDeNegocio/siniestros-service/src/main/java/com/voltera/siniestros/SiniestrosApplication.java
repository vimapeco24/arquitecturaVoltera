package com.voltera.siniestros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SiniestrosApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiniestrosApplication.class, args);
    }
}
