package com.pedidos360.email.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    void recibirMensaje_deberiaEnviarCorreo_siElMensajeEsValido() {
        String json = """
                {"destino":"cliente@pedidos360.cl",
                 "asunto":"Pedidos360 - Pedido #1 confirmado",
                 "cuerpo":"Tu pedido fue confirmado.",
                 "tipo":"PEDIDO_CONFIRMADO"}
                """;

        emailListener.recibirMensaje(mensajeConJson(json));

        verify(emailService).enviarCorreo(
                "cliente@pedidos360.cl",
                "Pedidos360 - Pedido #1 confirmado",
                "Tu pedido fue confirmado.");
    }

    @Test
    void recibirMensaje_deberiaDescartarElMensaje_siElJsonEsInvalido() {
        assertDoesNotThrow(() -> emailListener.recibirMensaje(mensajeConJson("{esto no es json")));
        verifyNoInteractions(emailService);
    }

    @Test
    void recibirMensaje_deberiaDescartarElMensaje_siElEnvioFalla() {
        doThrow(new RuntimeException("SMTP caído"))
                .when(emailService).enviarCorreo(anyString(), anyString(), anyString());
        String json = """
                {"destino":"cliente@pedidos360.cl","asunto":"Hola","cuerpo":"Texto","tipo":"PEDIDO_CONFIRMADO"}
                """;

        assertDoesNotThrow(() -> emailListener.recibirMensaje(mensajeConJson(json)));
    }
}
