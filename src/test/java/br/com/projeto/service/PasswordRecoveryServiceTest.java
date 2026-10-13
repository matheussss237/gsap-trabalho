package br.com.projeto.service;

import br.com.projeto.exception.NotFoundException;
import br.com.projeto.exception.ValidationException;
import br.com.projeto.support.InMemoryRepositories;
import br.com.projeto.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PasswordRecoveryServiceTest {

    private final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    private final InMemoryRepositories.ResetTokens tokens = new InMemoryRepositories.ResetTokens();
    private final SessionManager session = new SessionManager();
    private final UserService userService = new UserService(users, session);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-10T12:00:00Z"));
    private final PasswordRecoveryService service =
            new PasswordRecoveryService(userService, tokens, Optional.empty(), clock);
    private String userId;

    @BeforeEach
    void setUp() {
        userId = new AuthenticationService(userService, session)
                .register("Usuário Um", "usuario1@teste.com", "senhaAntiga1", "senhaAntiga1").getId();
    }

    @Test
    void fluxoCompletoTrocaASenhaEInvalidaOCodigo() {
        String code = service.requestReset("usuario1@teste.com").demoCode().orElseThrow();
        assertNotEquals(code, tokens.data.values().iterator().next().getTokenHash(), "só o hash é salvo");

        service.resetPassword("usuario1@teste.com", code, "senhaNova123", "senhaNova123");
        assertTrue(PasswordHasher.matches("senhaNova123", users.data.get(userId).getPasswordHash()));

        assertThrows(ValidationException.class,
                () -> service.resetPassword("usuario1@teste.com", code, "outra12345", "outra12345"));
    }

    @Test
    void codigoExpirado() {
        String code = service.requestReset("usuario1@teste.com").demoCode().orElseThrow();
        clock.advance(PasswordRecoveryService.TOKEN_VALIDITY.plusSeconds(1));
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.resetPassword("usuario1@teste.com", code, "senhaNova123", "senhaNova123"));
        assertTrue(e.getMessage().contains("expirou"));
    }

    @Test
    void tentativasErradasBloqueiamOCodigo() {
        String code = service.requestReset("usuario1@teste.com").demoCode().orElseThrow();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < PasswordRecoveryService.MAX_ATTEMPTS; i++) {
            assertThrows(ValidationException.class,
                    () -> service.resetPassword("usuario1@teste.com", wrong, "senhaNova123", "senhaNova123"));
        }
        assertThrows(ValidationException.class,
                () -> service.resetPassword("usuario1@teste.com", code, "senhaNova123", "senhaNova123"));
    }

    @Test
    void novoCodigoInvalidaOAnterior() {
        String first = service.requestReset("usuario1@teste.com").demoCode().orElseThrow();
        clock.advance(Duration.ofSeconds(5));
        String second = service.requestReset("usuario1@teste.com").demoCode().orElseThrow();
        if (!first.equals(second)) {
            assertThrows(ValidationException.class,
                    () -> service.resetPassword("usuario1@teste.com", first, "senhaNova123", "senhaNova123"));
        }
        assertDoesNotThrow(() -> service.resetPassword("usuario1@teste.com", second, "senhaNova123", "senhaNova123"));
    }

    @Test
    void emailInexistente() {
        assertThrows(NotFoundException.class, () -> service.requestReset("ninguem@teste.com"));
    }

    static class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
