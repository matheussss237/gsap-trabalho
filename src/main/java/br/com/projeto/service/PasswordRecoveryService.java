package br.com.projeto.service;

import br.com.projeto.config.AppConfig;
import br.com.projeto.model.PasswordResetToken;
import br.com.projeto.model.User;
import br.com.projeto.repository.PasswordResetRepository;
import br.com.projeto.util.AppException;
import br.com.projeto.util.Security;
import jakarta.mail.Authenticator;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

/**
 * Recuperação de senha por código temporário:
 * - a senha antiga nunca é exibida (só existe o hash BCrypt);
 * - código de 6 dígitos (SecureRandom), válido por 15 minutos e de uso único;
 * - no banco fica apenas o SHA-256 do código;
 * - 5 tentativas erradas invalidam o código.
 * Se SMTP_HOST/SMTP_USER/SMTP_PASSWORD estiverem configurados o código vai por e-mail;
 * senão a aplicação está em "modo demonstração" e o código é mostrado na tela.
 */
public class PasswordRecoveryService {

    public static final Duration CODE_VALIDITY = Duration.ofMinutes(15);
    public static final int MAX_ATTEMPTS = 5;

    /** sentByEmail = true quando o e-mail foi enviado; senão demoCode traz o código. */
    public record RequestResult(boolean sentByEmail, Optional<String> demoCode) {
    }

    private final AuthenticationService auth;
    private final PasswordResetRepository tokenRepository;
    private final Clock clock;

    public PasswordRecoveryService(AuthenticationService auth, PasswordResetRepository tokenRepository, Clock clock) {
        this.auth = auth;
        this.tokenRepository = tokenRepository;
        this.clock = clock;
    }

    public RequestResult requestReset(String email) {
        User user = auth.findByEmail(email)
                .orElseThrow(() -> new AppException("Não existe usuário cadastrado com este e-mail."));
        tokenRepository.invalidateAll(user.getId());
        String code = Security.newCode();
        Instant now = clock.instant();
        tokenRepository.save(new PasswordResetToken(user.getId(), Security.sha256(code), now, now.plus(CODE_VALIDITY)));

        if (smtpConfigured()) {
            sendEmail(user.getEmail(), "Lumi — código de recuperação de senha",
                    "Olá, " + user.firstName() + "!\n\nSeu código para redefinir a senha é: " + code
                            + "\n\nEle expira em " + CODE_VALIDITY.toMinutes() + " minutos e só pode ser usado uma vez."
                            + "\nSe você não pediu, ignore este e-mail.");
            return new RequestResult(true, Optional.empty());
        }
        return new RequestResult(false, Optional.of(code));
    }

    public void resetPassword(String email, String code, String newPassword, String confirmation) {
        String validCode = Security.required(code, "Código");
        Security.newPassword(newPassword, confirmation);
        User user = auth.findByEmail(email)
                .orElseThrow(() -> new AppException("Não existe usuário cadastrado com este e-mail."));
        PasswordResetToken token = tokenRepository.findLatestUnused(user.getId())
                .orElseThrow(() -> new AppException("Nenhum código ativo. Solicite um novo código."));

        if (token.isExpired(clock.instant())) {
            token.setUsed(true);
            tokenRepository.update(token);
            throw new AppException("O código expirou. Solicite um novo código.");
        }
        if (!Security.sameHash(token.getTokenHash(), Security.sha256(validCode))) {
            token.setAttempts(token.getAttempts() + 1);
            boolean blocked = token.getAttempts() >= MAX_ATTEMPTS;
            token.setUsed(blocked);
            tokenRepository.update(token);
            throw new AppException(blocked ? "Muitas tentativas incorretas. Solicite um novo código."
                    : "Código inválido. Restam " + (MAX_ATTEMPTS - token.getAttempts()) + " tentativa(s).");
        }
        auth.changePassword(user.getId(), newPassword);
        token.setUsed(true);
        tokenRepository.update(token);
    }

    public static boolean smtpConfigured() {
        return AppConfig.get("SMTP_HOST").isPresent() && AppConfig.get("SMTP_USER").isPresent()
                && AppConfig.get("SMTP_PASSWORD").isPresent();
    }

    /** Envio por SMTP com STARTTLS (ex.: Gmail com senha de app). */
    private void sendEmail(String to, String subject, String body) {
        String user = AppConfig.get("SMTP_USER", "");
        String password = AppConfig.get("SMTP_PASSWORD", "");
        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.get("SMTP_HOST", ""));
        props.put("mail.smtp.port", AppConfig.get("SMTP_PORT", "587"));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(AppConfig.get("SMTP_FROM", user), AppConfig.APP_NAME, "UTF-8"));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            Transport.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new AppException("Não foi possível enviar o e-mail de recuperação. Tente novamente mais tarde.", e);
        }
    }
}
