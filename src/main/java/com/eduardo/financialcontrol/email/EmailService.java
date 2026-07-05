package com.eduardo.financialcontrol.email;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void enviarEmailVerificacao(String destinatario, String token) {
        try {
            String link = frontendUrl + "/verificar.html?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(destinatario);
            message.setSubject("Confirme seu email - Controle Financeiro");
            message.setText(
                    "Olá!\n\n" +
                            "Para ativar sua conta no Controle Financeiro, clique no link abaixo:\n\n" +
                            link + "\n\n" +
                            "Este link expira em 24 horas.\n\n" +
                            "Se você não criou esta conta, ignore este email."
            );

            mailSender.send(message);
            log.info("Email de verificação enviado para {}", destinatario);
        } catch (Exception e) {
            log.error("Erro ao enviar email de verificação para {}: {}", destinatario, e.getMessage());
        }
    }
}