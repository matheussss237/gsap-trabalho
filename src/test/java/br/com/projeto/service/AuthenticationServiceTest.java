package br.com.projeto.service;

import br.com.projeto.exception.AuthenticationException;
import br.com.projeto.exception.ValidationException;
import br.com.projeto.model.User;
import br.com.projeto.support.TestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {

    private final TestContext ctx = new TestContext();
    private final AuthenticationService auth = ctx.app.authenticationService();

    @Test
    void cadastroValidoGuardaHashENaoASenha() {
        User user = auth.register("Usuário Um", "Usuario1@Teste.com", "senha1234", "senha1234");
        assertNotNull(user.getId());
        assertEquals("usuario1@teste.com", user.getEmail());
        assertNotEquals("senha1234", ctx.users.data.get(user.getId()).getPasswordHash());
    }

    @Test
    void cadastroDuplicadoEhRecusado() {
        auth.register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.register("Outro", "USUARIO1@teste.com", "senha1234", "senha1234"));
        assertTrue(e.getMessage().contains("Já existe"));
    }

    @Test
    void cadastroComCamposInvalidos() {
        assertThrows(ValidationException.class, () -> auth.register("", "a@b.com", "senha1234", "senha1234"));
        assertThrows(ValidationException.class, () -> auth.register("Ana", "email-invalido", "senha1234", "senha1234"));
        assertThrows(ValidationException.class, () -> auth.register("Ana", "a@b.com", "curta1", "curta1"));
        assertThrows(ValidationException.class, () -> auth.register("Ana", "a@b.com", "senha1234", "diferente1"));
    }

    @Test
    void loginComSenhaIncorreta() {
        auth.register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> auth.login("usuario1@teste.com", "errada123"));
        assertTrue(e.getMessage().contains("Senha incorreta"));
        assertFalse(ctx.app.session().isAuthenticated());
    }

    @Test
    void loginComUsuarioInexistente() {
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> auth.login("ninguem@teste.com", "senha1234"));
        assertTrue(e.getMessage().contains("Não existe usuário"));
    }

    @Test
    void loginValidoIniciaSessaoELogoutEncerra() {
        auth.register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        User logged = auth.login("usuario1@teste.com", "senha1234");
        assertEquals(logged.getId(), ctx.app.session().requireUserId());

        auth.logout();
        assertFalse(ctx.app.session().isAuthenticated());
        assertThrows(AuthenticationException.class, () -> ctx.app.conversationService().listMine());
    }
}
