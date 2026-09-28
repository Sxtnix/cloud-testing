package com.pedidos360.carrito.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conexión con RabbitMQ: la cola de correos se declara aquí (lado productor)
 * y también en ms-email (lado consumidor), para que funcione en cualquier
 * orden de arranque.
 */
@Configuration
public class RabbitConfig {

    public static final String EMAIL_QUEUE = "pedidos360.emails";

    @Bean
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }

    /**
     * Serializa los mensajes como JSON. ms-email hace un parseo directo del
     * cuerpo, por lo que solo intercambia el formato JSON por la cola.
     */
    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
