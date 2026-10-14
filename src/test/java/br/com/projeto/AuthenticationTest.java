package br.com.projeto;

import br.com.projeto.model.User;
import br.com.projeto.util.AppException;
import br.com.projeto.util.Security;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Cadastro, login, logout, sessão e segurança das senhas. */
class AuthenticationTest {

    private final TestSupport app = new TestSupport();

    @Test
    void cadastroValidoGuardaHashENaoASenha() {
        User user = app.auth.register("Usuário Um", "Usuario1@Teste.com", "senha1234", "senha1234");
        assertNotNull(user.getId());
        assertEquals("usuario1@teste.com", user.getEmail());
        String hash = app.users.data.get(user.getId()).getPasswordHash();
        assertNotEquals("senha1234", hash);
        assertTrue(hash.startsWith("$2a$"));
    }

    @Test
    void cadastroDuplicadoEhRecusado() {
        app.auth.register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        AppException e = assertThrows(AppException.class,
                () -> app.auth.register("Outro", "USUARIO1@teste.com", "senha1234", "senha1234"));
        assertTrue(e.getMessage().contains("Já existe"));
    }

    @Test
    void cadastroComCamposInvalidos() {
        assertThrows(AppException.class, () -> app.auth.register("", "a@b.com", "senha1234", "senha1234"));
        assertThrows(AppException.class, () -> app.auth.register("Ana", "email-invalido", "senha1234", "senha1234"));
        assertThrows(AppException.class, () -> app.auth.register("Ana", "a@b.com", "curta1", "curta1"));
        assertThrows(AppException.class, () -> app.auth.register("Ana", "a@b.com", "somenteletras", "somenteletras"));
        assertThrows(AppException.class, () -> app.auth.register("Ana", "a@b.com", "senha1234", "diferente1"));
    }

    @Test
    void senhaIncorreta() {
        app.auth.register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        AppException e = assertThrows(AppException.class, () -> app.auth.login("usuario1@teste.com", "errada123"));
        assertTrue(e.getMessage().contains("Senha incorreta"));
        assertTrue(app.auth.currentUser().isEmpty());
    }

    @Test
    void usuarioInexistente() {
        AppException e = assertThrows(AppException.class, () -> app.auth.login("ninguem@teste.com", "senha1234"));
        assertTrue(e.getMessage().contains("Não existe usuário"));
    }

    @Test
    void loginIniciaSessaoELogoutEncerra() {
        app.auth.register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        User logged = app.auth.login("usuario1@teste.com", "senha1234");
        assertEquals(logged.getId(), app.auth.requireUserId());
        app.auth.logout();
        assertTrue(app.auth.currentUser().isEmpty());
        assertThrows(AppException.class, () -> app.conversations.listMine());
    }

    @Test
    void hashComSaltEValidacoes() {
        assertNotEquals(Security.hashPassword("abc12345"), Security.hashPassword("abc12345"));
        assertTrue(Security.checkPassword("abc12345", Security.hashPassword("abc12345")));
        assertEquals("ana@empresa.com", Security.email("  Ana@Empresa.com "));
        assertTrue(Security.newCode().matches("\\d{6}"));
    }
}
