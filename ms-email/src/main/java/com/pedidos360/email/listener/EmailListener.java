package com.pedidos360.email.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.email.config.RabbitConfig;
import com.pedidos360.email.model.MensajeEmail;
import com.pedidos360.email.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de la cola de correos: recibe los eventos publicados por
 * ms-carrito (ej. PEDIDO_CONFIRMADO) y dispara el envío por SMTP.
 *
 * El cuerpo del mensaje se parsea como JSON a MensajeEmail en vez de usar
 * la conversión por tipo de Spring AMQP, para no acoplarnos al classpath
 * del productor (los dos servicios son desplegables independientes).
 */
@Component
public class EmailListener {

    private static final Logger log = LoggerFactory.getLogger(EmailListener.class);

    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    public EmailListener(ObjectMapper objectMapper, EmailService emailService) {
        this.objectMapper = objectMapper;
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void recibirMensaje(Message message) {
        MensajeEmail mensaje;
        try {
            mensaje = objectMapper.readValue(message.getBody(), MensajeEmail.class);
        } catch (IOException e) {
            // Mensaje corrupto: se descarta para no entrar en un bucle de reintentos.
            log.error("Mensaje inválido en la cola {}: {}", RabbitConfig.EMAIL_QUEUE, e.getMessage());
            return;
        }

        try {
            emailService.enviarCorreo(mensaje.destino(), mensaje.asunto(), mensaje.cuerpo());
            log.info("Mensaje de tipo {} procesado -> {}", mensaje.tipo(), mensaje.destino());
        } catch (Exception e) {
            // Error de envío (p. ej. SMTP caído): se registra y se descarta el mensaje
            // para evitar un bucle infinito de reentregas. En producción aquí convendría
            // una cola de respaldo (DLQ) con reintentos acotados.
            log.error("No se pudo enviar el correo a {}: {}", mensaje.destino(), e.getMessage(), e);
        }
    }
}
