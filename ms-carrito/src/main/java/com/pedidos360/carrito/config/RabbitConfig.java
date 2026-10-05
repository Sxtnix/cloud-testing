package com.pedidos360.carrito.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conexion con RabbitMQ: la cola de correos se declara aqui (lado productor)
 * y tambien en ms-email (lado consumidor), para que funcione en cualquier
 * orden de arranque.
 *
 * La cola principal esta configurada con dead-letter: si ms-email agota sus
 * reintentos, el mensaje no se pierde ni entra en bucle, sino que termina en
 * la cola de respaldo pedidos360.emails.dlq para poder inspeccionarlo.
 * Ambos servicios declaran EXACTAMENTE los mismos argumentos para evitar
 * PRECONDITION_FAILED en RabbitMQ.
 */
@Configuration
public class RabbitConfig {

    public static final String EMAIL_QUEUE = "pedidos360.emails";
    public static final String EMAIL_DEAD_LETTER_QUEUE = "pedidos360.emails.dlq";

    @Bean
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", EMAIL_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DEAD_LETTER_QUEUE).build();
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
