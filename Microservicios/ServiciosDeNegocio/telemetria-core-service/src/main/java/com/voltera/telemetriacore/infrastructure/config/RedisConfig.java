package com.voltera.telemetriacore.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Configuracion de Redis para el lado QUERY (lamina 03 · CQRS: "Vista de lectura ·
 * Redis (en vivo) + vistas agregadas"). Se activa SOLO con el perfil {@code redis};
 * en el perfil por defecto no se crea ninguna conexion y se usan los adaptadores
 * in-memory. Los beans se declaran a mano porque la autoconfiguracion de Redis se
 * excluye en {@code TelemetriaCoreApplication}.
 */
@Configuration
@Profile("redis")
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
