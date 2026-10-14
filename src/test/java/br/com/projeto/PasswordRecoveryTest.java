package br.com.projeto;

import br.com.projeto.service.PasswordRecoveryService;
import br.com.projeto.util.AppException;
import br.com.projeto.util.Security;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** Recuperação de senha com código temporário. */
class PasswordRecoveryTest {

    private final TestSupport app = new TestSupport();
    private String userId;

    @BeforeEach
    void setUp() {
        userId = app.auth.register("Usuário Um", "usuario1@teste.com", "senhaAntiga1", "senhaAntiga1").getId();
    }

    private String requestCode() {
        return app.recovery.requestReset("usuario1@teste.com").demoCode().orElseThrow();
    }

    @Test
    void fluxoCompletoTrocaASenhaEOCodigoNaoServeDuasVezes() {
        String code = requestCode();
        assertNotEquals(code, app.tokens.data.values().iterator().next().getTokenHash(), "só o hash é salvo");
        app.recovery.resetPassword("usuario1@teste.com", code, "senhaNova123", "senhaNova123");
        assertTrue(Security.checkPassword("senhaNova123", app.users.data.get(userId).getPasswordHash()));
        assertThrows(AppException.class,
                () -> app.recovery.resetPassword("usuario1@teste.com", code, "outra12345", "outra12345"));
    }

    @Test
    void codigoExpirado() {
        String code = requestCode();
        app.clock.advance(PasswordRecoveryService.CODE_VALIDITY.plusSeconds(1));
        AppException e = assertThrows(AppException.class,
                () -> app.recovery.resetPassword("usuario1@teste.com", code, "senhaNova123", "senhaNova123"));
        assertTrue(e.getMessage().contains("expirou"));
    }

    @Test
    void cincoTentativasErradasBloqueiamOCodigo() {
        String code = requestCode();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < PasswordRecoveryService.MAX_ATTEMPTS; i++) {
            assertThrows(AppException.class,
                    () -> app.recovery.resetPassword("usuario1@teste.com", wrong, "senhaNova123", "senhaNova123"));
        }
        assertThrows(AppException.class,
                () -> app.recovery.resetPassword("usuario1@teste.com", code, "senhaNova123", "senhaNova123"));
    }

    @Test
    void novoCodigoInvalidaOAnterior() {
        String first = requestCode();
        app.clock.advance(Duration.ofSeconds(5));
        String second = requestCode();
        if (!first.equals(second)) {
            assertThrows(AppException.class,
                    () -> app.recovery.resetPassword("usuario1@teste.com", first, "senhaNova123", "senhaNova123"));
        }
        assertDoesNotThrow(() -> app.recovery.resetPassword("usuario1@teste.com", second, "senhaNova123", "senhaNova123"));
    }

    @Test
    void emailInexistente() {
        assertThrows(AppException.class, () -> app.recovery.requestReset("ninguem@teste.com"));
    }
}
