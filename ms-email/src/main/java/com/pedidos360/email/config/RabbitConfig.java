package com.pedidos360.email.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cola donde ms-carrito publica los correos pendientes.
 * La cola se declara en ambos lados (productor y consumidor) para que
 * funcione aunque alguno de los dos se levante primero.
 */
@Configuration
public class RabbitConfig {

    public static final String EMAIL_QUEUE = "pedidos360.emails";

    @Bean
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }
}
