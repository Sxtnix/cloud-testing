package com.pedidos360.carrito.messaging;

import com.pedidos360.carrito.config.RabbitConfig;
import com.pedidos360.carrito.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de email en RabbitMQ. El envio real del correo lo hace
 * ms-email de forma asincrona, desacoplando la compra del estado del SMTP.
 *
 * Eventos publicados (uno por hecho de negocio, para no duplicar correos):
 *   PEDIDO_REGISTRADO -> checkout: el pedido queda pendiente de pago
 *   PAGO_APROBADO     -> el pago simulado fue aprobado
 *   PAGO_RECHAZADO    -> el pago simulado fue rechazado
 */
@Component
public class EmailPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmailPublisher.class);

    public static final String TIPO_PEDIDO_REGISTRADO = "PEDIDO_REGISTRADO";
    public static final String TIPO_PAGO_APROBADO = "PAGO_APROBADO";
    public static final String TIPO_PAGO_RECHAZADO = "PAGO_RECHAZADO";
    public static final String TIPO_CAMBIO_ESTADO = "CAMBIO_ESTADO";

    private final RabbitTemplate rabbitTemplate;

    public EmailPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Confirmacion de compra: el pedido ya quedo registrado en el sistema.
     */
    public void publicarPedidoRegistrado(String destino, String nombre, Pedido pedido) {
        String cuerpo = PlantillaEmails.envoltorio(
                "Pedido #" + pedido.getId() + " registrado",
                PlantillaEmails.resumenPedido(pedido, nombre)
                        + PlantillaEmails.cierre(
                                "Recibimos tu pedido y ya lo estamos preparando. "
                                        + "El pago aún no se ha procesado: cuando lo hagas recibirás la confirmación. "
                                        + "Gracias por comprar en Pedidos360."));
        publicar(destino, pedido, "Pedidos360 - Pedido #" + pedido.getId() + " registrado",
                cuerpo, TIPO_PEDIDO_REGISTRADO);
    }

    /**
     * Confirmacion de pago aprobado.
     */
    public void publicarPagoAprobado(String destino, String nombre, Pedido pedido) {
        String cuerpo = PlantillaEmails.envoltorio(
                "Pago aprobado - Pedido #" + pedido.getId(),
                PlantillaEmails.aviso("#dcfce7", "Tu pago fue aprobado. El pedido está confirmado.")
                        + PlantillaEmails.resumenPedido(pedido, nombre)
                        + PlantillaEmails.cierre("Gracias por comprar en Pedidos360."));
        publicar(destino, pedido, "Pedidos360 - Pago aprobado (pedido #" + pedido.getId() + ")",
                cuerpo, TIPO_PAGO_APROBADO);
    }

    /**
     * Aviso de pago rechazado: el pedido queda cancelado y el carrito restaurado.
     */
    public void publicarPagoRechazado(String destino, String nombre, Pedido pedido) {
        String cuerpo = PlantillaEmails.envoltorio(
                "Pago rechazado - Pedido #" + pedido.getId(),
                PlantillaEmails.aviso("#fee2e2", "Tu pago fue rechazado. El pedido quedó cancelado.")
                        + PlantillaEmails.resumenPedido(pedido, nombre)
                        + PlantillaEmails.cierre(
                                "No se realizó ningún cobro. Tus productos siguen en el carrito "
                                        + "para que puedas intentarlo nuevamente."));
        publicar(destino, pedido, "Pedidos360 - Pago rechazado (pedido #" + pedido.getId() + ")",
                cuerpo, TIPO_PAGO_RECHAZADO);
    }

    /**
     * Actualizacion del estado del pedido (seguimiento logistico): un unico
     * correo por cada cambio de estado, no uno por evento repetido.
     */
    public void publicarCambioEstado(String destino, String nombre, Pedido pedido) {
        String color = switch (pedido.getEstado()) {
            case ENTREGADO, CONFIRMADO -> "#dcfce7";
            case CANCELADO -> "#fee2e2";
            default -> "#dbeafe";
        };

        String cuerpo = PlantillaEmails.envoltorio(
                "Actualizacion del pedido #" + pedido.getId(),
                PlantillaEmails.aviso(color,
                        "El estado de tu pedido cambio a: " + PlantillaEmails.etiquetaEstado(pedido))
                        + PlantillaEmails.resumenPedido(pedido, nombre)
                        + PlantillaEmails.cierre(
                                "Puedes consultar el detalle de tus compras en la seccion "
                                        + "\"Mis pedidos\" de Pedidos360."));

        publicar(destino, pedido, "Pedidos360 - Pedido #" + pedido.getId() + ": "
                        + PlantillaEmails.etiquetaEstado(pedido),
                cuerpo, TIPO_CAMBIO_ESTADO);
    }

    /**
     * Publica el mensaje en la cola de correos.
     * Si RabbitMQ no esta disponible solo se registra el error: el pedido ya
     * quedo persistido y no debe fallar por el envio del correo.
     */
    private void publicar(String destino, Pedido pedido, String asunto, String cuerpo, String tipo) {
        if (destino == null || destino.isBlank()) {
            log.warn("No se publica el evento {} del pedido {}: el token no incluye email (preferred_username)",
                    tipo, pedido.getId());
            return;
        }

        EmailMessage mensaje = new EmailMessage(destino, asunto, cuerpo, tipo);

        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, mensaje);
            log.info("Evento {} del pedido {} publicado en la cola {} -> {}",
                    tipo, pedido.getId(), RabbitConfig.EMAIL_QUEUE, destino);
        } catch (AmqpException e) {
            log.error("No se pudo publicar el evento {} del pedido {}: {}",
                    tipo, pedido.getId(), e.getMessage(), e);
        }
    }
}
