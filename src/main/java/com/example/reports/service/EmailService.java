package com.example.reports.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import javax.mail.MessagingException;
import java.nio.file.Paths;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarCorreo(String destinatarios, String asunto, String cuerpo, String zipFilePath) throws MessagingException, jakarta.mail.MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(destinatarios.split(","));
        helper.setSubject(asunto);  // Asunto del correo
        helper.setText(cuerpo);  // Cuerpo del correo
        helper.addAttachment("reporte.zip", Paths.get(zipFilePath).toFile());
        mailSender.send(message);
    }
}
