package com.pedidos360.email.model;

/**
 * Mensaje que viaja por la cola RabbitMQ pedidos360.emails.
 * Debe mantener los mismos campos que com.pedidos360.carrito.messaging.EmailMessage
 * (el consumidor hace parseo directo del JSON, sin acoplarse a las clases del productor).
 */
public record MensajeEmail(
        String destino,
        String asunto,
        String cuerpo,
        String tipo
) {
}
