package br.com.projeto.service;

import br.com.projeto.config.AppConfig;
import br.com.projeto.exception.AppException;
import jakarta.mail.Authenticator;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

/** Envio por SMTP com STARTTLS. Credenciais vêm de SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD. */
public class SmtpEmailService implements EmailService {

    private final String host;
    private final String port;
    private final String user;
    private final String password;
    private final String from;

    private SmtpEmailService(String host, String port, String user, String password, String from) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.from = from;
    }

    /** Disponível apenas se as variáveis SMTP estiverem configuradas. */
    public static Optional<EmailService> fromConfig() {
        Optional<String> host = AppConfig.get(AppConfig.SMTP_HOST);
        Optional<String> user = AppConfig.get(AppConfig.SMTP_USER);
        Optional<String> password = AppConfig.get(AppConfig.SMTP_PASSWORD);
        if (host.isEmpty() || user.isEmpty() || password.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SmtpEmailService(host.get(), AppConfig.getOrDefault(AppConfig.SMTP_PORT, "587"),
                user.get(), password.get(), AppConfig.getOrDefault(AppConfig.SMTP_FROM, user.get())));
    }

    @Override
    public void send(String to, String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from, AppConfig.APP_NAME, StandardCharsets.UTF_8.name()));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setText(body, StandardCharsets.UTF_8.name());
            Transport.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new AppException("Não foi possível enviar o e-mail de recuperação. Tente novamente mais tarde.", e);
        }
    }
}
