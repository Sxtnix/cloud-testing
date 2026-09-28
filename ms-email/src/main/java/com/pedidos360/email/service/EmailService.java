package com.pedidos360.email.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /** Remitente configurado en application.yml (MAIL_FROM). */
    @Value("${pedidos360.email.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo de texto plano por SMTP.
     * Lanza IllegalArgumentException si el destino falta y
     * org.springframework.mail.MailException si el servidor SMTP rechaza el envío.
     */
    public void enviarCorreo(String destino, String asunto, String cuerpo) {
        if (destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("El destino del correo es obligatorio");
        }
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("El asunto del correo es obligatorio");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destino);
        message.setSubject(asunto);
        message.setText(cuerpo);
        message.setFrom(from);

        mailSender.send(message);
        log.info("Correo enviado a {} (asunto: {})", destino, asunto);
    }
}
