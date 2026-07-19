package com.asus.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mail. Se houver SMTP configurado ({@code spring.mail.host}), envia de verdade;
 * senao, apenas registra o conteudo no log (modo dev/sem provedor) — assim o fluxo de
 * verificacao funciona e o link fica vissivel no console ate plugar um provedor real.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${asus.mail.from:no-reply@asus-rpg.local}")
    private String remetente;

    public EmailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    /** True quando ha um provedor SMTP configurado (bean JavaMailSender presente). */
    public boolean configurado() {
        return mailSender.getIfAvailable() != null;
    }

    public void enviar(String para, String assunto, String corpo) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("[E-MAIL NAO CONFIGURADO] Para: {} | Assunto: {}\n{}\n"
                    + "(defina spring.mail.host/username/password para enviar de verdade)", para, assunto, corpo);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(para);
            msg.setSubject(assunto);
            msg.setText(corpo);
            if (remetente != null && !remetente.isBlank()) {
                msg.setFrom(remetente);
            }
            sender.send(msg);
            log.info("E-mail enviado para {}: {}", para, assunto);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail para {}: {}", para, e.getMessage());
        }
    }
}
