package com.pedidos360.carrito.messaging;

/**
 * Mensaje publicado en la cola RabbitMQ pedidos360.emails.
 * Debe mantener los mismos campos que com.pedidos360.email.model.MensajeEmail
 * (el consumidor hace parseo directo del JSON).
 */
public record EmailMessage(
        String destino,
        String asunto,
        String cuerpo,
        String tipo
) {
}
