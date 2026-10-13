package br.com.projeto.util;

import br.com.projeto.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @Test
    void hashNaoGuardaSenhaEmTextoPuroEValida() {
        String hash = PasswordHasher.hash("senhaForte123");
        assertNotEquals("senhaForte123", hash);
        assertTrue(hash.startsWith("$2a$"));
        assertTrue(PasswordHasher.matches("senhaForte123", hash));
        assertFalse(PasswordHasher.matches("outraSenha123", hash));
    }

    @Test
    void mesmoTextoGeraHashesDiferentes() {
        assertNotEquals(PasswordHasher.hash("abc12345"), PasswordHasher.hash("abc12345"));
    }

    @Test
    void codigoTemSeisDigitos() {
        for (int i = 0; i < 50; i++) {
            assertTrue(TokenGenerator.numericCode().matches("\\d{6}"));
        }
    }

    @Test
    void validaEmailESenha() {
        assertEquals("ana@empresa.com", Validator.email("  Ana@Empresa.com "));
        assertThrows(ValidationException.class, () -> Validator.email("ana@"));
        assertThrows(ValidationException.class, () -> Validator.newPassword("abc", "abc"));
        assertThrows(ValidationException.class, () -> Validator.newPassword("somenteletras", "somenteletras"));
        assertThrows(ValidationException.class, () -> Validator.newPassword("senha1234", "senha12345"));
        assertDoesNotThrow(() -> Validator.newPassword("senha1234", "senha1234"));
    }
}
