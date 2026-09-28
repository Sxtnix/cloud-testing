package com.pedidos360.email.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "from", "no-reply@pedidos360.cl");
    }

    @Test
    void enviarCorreo_deberiaEnviarMensajeConDestinoAsuntoYCuerpo() {
        emailService.enviarCorreo("cliente@pedidos360.cl", "Pedido confirmado", "Tu pedido fue confirmado.");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage mensaje = captor.getValue();
        assertEquals("cliente@pedidos360.cl", mensaje.getTo()[0]);
        assertEquals("Pedido confirmado", mensaje.getSubject());
        assertEquals("Tu pedido fue confirmado.", mensaje.getText());
        assertEquals("no-reply@pedidos360.cl", mensaje.getFrom());
    }

    @Test
    void enviarCorreo_deberiaLanzarExcepcion_siDestinoEsInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.enviarCorreo("  ", "Asunto", "Cuerpo"));
        assertThrows(IllegalArgumentException.class,
                () -> emailService.enviarCorreo(null, "Asunto", "Cuerpo"));
        verifyNoInteractions(mailSender);
    }

    @Test
    void enviarCorreo_deberiaLanzarExcepcion_siAsuntoEsInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.enviarCorreo("cliente@pedidos360.cl", "", "Cuerpo"));
        verifyNoInteractions(mailSender);
    }
}
