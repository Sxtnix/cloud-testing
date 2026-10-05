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
 * ms-carrito (PEDIDO_REGISTRADO, PAGO_APROBADO, PAGO_RECHAZADO) y dispara el
 * envio por SMTP.
 *
 * Si el envio falla (SMTP caido, mensaje corrupto) se lanza la excepcion para
 * que el contenedor del listener reintente (spring.rabbitmq.listener.simple.retry)
 * y, agotados los intentos, el mensaje termine en la cola de respaldo
 * pedidos360.emails.dlq. Asi el correo no se pierde y tampoco entra en bucle
 * infinito de reentregas. El resto de la compra nunca falla: el pedido ya esta
 * persistido en ms-carrito.
 *
 * El cuerpo del mensaje se parsea como JSON a MensajeEmail en lugar de usar
 * la conversion por tipo de Spring AMQP, para no acoplarnos al classpath del
 * productor (los dos servicios son desplegables independientes).
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
            // Mensaje ilegible: se lanza para que quede en la cola de respaldo
            // y pueda inspeccionarse, en lugar de descartarlo en silencio.
            log.error("Mensaje invalido en la cola {}: {}", RabbitConfig.EMAIL_QUEUE, e.getMessage());
            throw new IllegalStateException("Mensaje invalido en la cola " + RabbitConfig.EMAIL_QUEUE, e);
        }

        try {
            if (EmailService.esHtml(mensaje.cuerpo())) {
                emailService.enviarHtml(mensaje.destino(), mensaje.asunto(), mensaje.cuerpo());
            } else {
                emailService.enviarCorreo(mensaje.destino(), mensaje.asunto(), mensaje.cuerpo());
            }
            log.info("Mensaje de tipo {} procesado -> {}", mensaje.tipo(), mensaje.destino());
        } catch (Exception e) {
            // Error de envio (p. ej. SMTP caido): se propaga para que el contenedor
            // reintente un numero acotado de veces y luego pase a la DLQ.
            log.error("No se pudo enviar el correo a {}: {}", mensaje.destino(), e.getMessage(), e);
            throw new IllegalStateException(
                    "No se pudo enviar el correo a " + mensaje.destino() + ": " + e.getMessage(), e);
        }
    }
}
