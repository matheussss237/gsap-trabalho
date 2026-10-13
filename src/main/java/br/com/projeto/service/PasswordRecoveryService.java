package br.com.projeto.service;

import br.com.projeto.exception.NotFoundException;
import br.com.projeto.exception.ValidationException;
import br.com.projeto.model.PasswordResetToken;
import br.com.projeto.model.User;
import br.com.projeto.repository.PasswordResetRepository;
import br.com.projeto.util.TokenGenerator;
import br.com.projeto.util.Validator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Recuperação de senha por código temporário.
 * - a senha antiga nunca é exibida (só existe o hash BCrypt);
 * - é gerado um código de 6 dígitos com SecureRandom, válido por 15 minutos e de uso único;
 * - no banco fica apenas o SHA-256 do código;
 * - após 5 tentativas erradas o código é invalidado.
 */
public class PasswordRecoveryService {

    public static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);
    public static final int MAX_ATTEMPTS = 5;

    private final UserService userService;
    private final PasswordResetRepository tokenRepository;
    private final Optional<EmailService> emailService;
    private final Clock clock;

    public PasswordRecoveryService(UserService userService, PasswordResetRepository tokenRepository,
                                   Optional<EmailService> emailService, Clock clock) {
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.clock = clock;
    }

    /**
     * Resultado da solicitação. Se não houver SMTP configurado, a aplicação está em
     * "modo demonstração" e o código é devolvido para ser exibido na tela.
     */
    public record RequestResult(boolean sentByEmail, Optional<String> demoCode) {
    }

    public RequestResult requestReset(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Não existe usuário cadastrado com este e-mail."));

        tokenRepository.invalidateAll(user.getId());
        String code = TokenGenerator.numericCode();
        Instant now = clock.instant();
        tokenRepository.save(new PasswordResetToken(user.getId(), TokenGenerator.sha256(code), now,
                now.plus(TOKEN_VALIDITY)));

        if (emailService.isPresent()) {
            emailService.get().send(user.getEmail(), "Nexus Chat — código de recuperação de senha",
                    "Olá, " + user.getName() + "!\n\nSeu código para redefinir a senha é: " + code
                            + "\n\nEle expira em " + TOKEN_VALIDITY.toMinutes() + " minutos e só pode ser usado uma vez."
                            + "\nSe você não solicitou, ignore este e-mail.");
            return new RequestResult(true, Optional.empty());
        }
        return new RequestResult(false, Optional.of(code));
    }

    public void resetPassword(String email, String code, String newPassword, String confirmation) {
        String validCode = Validator.required(code, "Código");
        Validator.newPassword(newPassword, confirmation);

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Não existe usuário cadastrado com este e-mail."));
        PasswordResetToken token = tokenRepository.findLatestUnused(user.getId())
                .orElseThrow(() -> new ValidationException("Nenhum código ativo. Solicite um novo código."));

        if (token.isExpired(clock.instant())) {
            token.setUsed(true);
            tokenRepository.update(token);
            throw new ValidationException("O código expirou. Solicite um novo código.");
        }
        if (!TokenGenerator.constantTimeEquals(token.getTokenHash(), TokenGenerator.sha256(validCode))) {
            token.setAttempts(token.getAttempts() + 1);
            if (token.getAttempts() >= MAX_ATTEMPTS) {
                token.setUsed(true);
                tokenRepository.update(token);
                throw new ValidationException("Muitas tentativas incorretas. Solicite um novo código.");
            }
            tokenRepository.update(token);
            throw new ValidationException("Código inválido. Restam "
                    + (MAX_ATTEMPTS - token.getAttempts()) + " tentativa(s).");
        }

        userService.setPassword(user.getId(), newPassword);
        token.setUsed(true);
        tokenRepository.update(token);
    }
}
