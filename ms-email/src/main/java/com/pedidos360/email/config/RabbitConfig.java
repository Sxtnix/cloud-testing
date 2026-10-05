package com.pedidos360.email.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cola donde ms-carrito publica los correos pendientes.
 * La cola se declara en ambos lados (productor y consumidor) para que
 * funcione aunque alguno de los dos se levante primero.
 *
 * x-dead-letter-*: si el envio agota los reintentos configurados en
 * application.yml, el mensaje termina en pedidos360.emails.dlq en lugar de
 * perderse o entrar en un bucle de reentregas. Ambos servicios declaran los
 * mismos argumentos para evitar PRECONDITION_FAILED.
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
}
