package com.pedidos360.carrito.messaging;

import com.pedidos360.carrito.config.RabbitConfig;
import com.pedidos360.carrito.model.ItemPedido;
import com.pedidos360.carrito.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de email en RabbitMQ. El envío real del correo lo hace
 * ms-email de forma asíncrona, desacoplando la compra del estado del SMTP.
 */
@Component
public class EmailPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmailPublisher.class);

    public static final String TIPO_PEDIDO_CONFIRMADO = "PEDIDO_CONFIRMADO";

    private final RabbitTemplate rabbitTemplate;

    public EmailPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica la confirmación de un pedido en la cola de correos.
     * Si RabbitMQ no está disponible solo se registra el error: el checkout
     * ya quedó persistido y no debe fallar por el envío del correo.
     */
    public void publicarPedidoConfirmado(String destino, String nombre, Pedido pedido) {
        if (destino == null || destino.isBlank()) {
            log.warn("No se publica la confirmación del pedido {}: el token no incluye email (preferred_username)",
                    pedido.getId());
            return;
        }

        EmailMessage mensaje = new EmailMessage(
                destino,
                "Pedidos360 - Pedido #" + pedido.getId() + " confirmado",
                construirCuerpo(nombre, pedido),
                TIPO_PEDIDO_CONFIRMADO
        );

        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, mensaje);
            log.info("Confirmación del pedido {} publicada en la cola {} -> {}",
                    pedido.getId(), RabbitConfig.EMAIL_QUEUE, destino);
        } catch (AmqpException e) {
            log.error("No se pudo publicar la confirmación del pedido {}: {}",
                    pedido.getId(), e.getMessage(), e);
        }
    }

    private String construirCuerpo(String nombre, Pedido pedido) {
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Hola ").append(nombre == null || nombre.isBlank() ? "cliente" : nombre).append(",\n\n");
        cuerpo.append("¡Tu pedido N° ").append(pedido.getId()).append(" fue confirmado!\n\n");
        cuerpo.append("Detalle:\n");

        for (ItemPedido item : pedido.getItems()) {
            cuerpo.append(" - ")
                    .append(item.getCantidad()).append(" x ")
                    .append(item.getNombreProducto())
                    .append(" - $").append(item.getPrecioUnitario() * item.getCantidad())
                    .append("\n");
        }

        cuerpo.append("\nTotal: $").append(pedido.getTotal()).append("\n");
        cuerpo.append("Gracias por comprar en Pedidos360.");
        return cuerpo.toString();
    }
}
