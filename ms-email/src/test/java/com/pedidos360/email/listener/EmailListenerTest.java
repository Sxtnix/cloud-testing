package com.pedidos360.email.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EmailListenerTest {

    private EmailService emailService;
    private EmailListener emailListener;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        emailListener = new EmailListener(new ObjectMapper(), emailService);
    }

    private Message mensajeConJson(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void recibirMensaje_deberiaEnviarCorreoDeTexto_siElCuerpoNoEsHtml() {
        String json = """
                {"destino":"cliente@pedidos360.cl",
                 "asunto":"Pedidos360 - Pedido #1 registrado",
                 "cuerpo":"Tu pedido fue registrado.",
                 "tipo":"PEDIDO_REGISTRADO"}
                """;

        emailListener.recibirMensaje(mensajeConJson(json));

        verify(emailService).enviarCorreo(
                "cliente@pedidos360.cl",
                "Pedidos360 - Pedido #1 registrado",
                "Tu pedido fue registrado.");
        verify(emailService, never()).enviarHtml(anyString(), anyString(), anyString());
    }

    @Test
    void recibirMensaje_deberiaEnviarHtml_siElCuerpoEsPlantillaHtml() {
        String json = """
                {"destino":"cliente@pedidos360.cl",
                 "asunto":"Pedidos360 - Pago aprobado",
                 "cuerpo":"<!doctype html><html><body>Hola</body></html>",
                 "tipo":"PAGO_APROBADO"}
                """;

        emailListener.recibirMensaje(mensajeConJson(json));

        verify(emailService).enviarHtml(
                "cliente@pedidos360.cl",
                "Pedidos360 - Pago aprobado",
                "<!doctype html><html><body>Hola</body></html>");
        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
    }

    @Test
    void recibirMensaje_deberiaLanzarExcepcion_siElJsonEsInvalido() {
        // La excepcion hace que el contenedor reintente y luego pase el mensaje
        // a la cola de respaldo, en vez de perderlo en silencio.
        assertThrows(IllegalStateException.class,
                () -> emailListener.recibirMensaje(mensajeConJson("{esto no es json")));
        verifyNoInteractions(emailService);
    }

    @Test
    void recibirMensaje_deberiaLanzarExcepcion_siElEnvioFalla() {
        doThrow(new RuntimeException("SMTP caido"))
                .when(emailService).enviarCorreo(anyString(), anyString(), anyString());
        String json = """
                {"destino":"cliente@pedidos360.cl","asunto":"Hola","cuerpo":"Texto","tipo":"PEDIDO_REGISTRADO"}
                """;

        assertThrows(IllegalStateException.class, () -> emailListener.recibirMensaje(mensajeConJson(json)));
    }
}
