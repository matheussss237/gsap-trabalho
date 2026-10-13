package br.com.projeto.service;

import br.com.projeto.exception.NotFoundException;
import br.com.projeto.model.Conversation;
import br.com.projeto.support.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** usuario1 ≠ conversas de usuario2 — o isolamento é garantido nos Services/Repositories. */
class ConversationIsolationTest {

    private final TestContext ctx = new TestContext();
    private final AuthenticationService auth = ctx.app.authenticationService();
    private final ConversationService conversations = ctx.app.conversationService();

    @BeforeEach
    void criaUsuarios() {
        auth.register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        auth.register("Usuário Dois", "usuario2@teste.com", "senha1234", "senha1234");
    }

    @Test
    void cadaUsuarioVeApenasSuasConversas() {
        auth.login("usuario1@teste.com", "senha1234");
        Conversation java = conversations.rename(conversations.create().getId(), "Java");
        conversations.rename(conversations.create().getId(), "JavaFX");
        auth.logout();

        auth.login("usuario2@teste.com", "senha1234");
        conversations.rename(conversations.create().getId(), "Angular");
        List<String> titles2 = conversations.listMine().stream().map(Conversation::getTitle).toList();
        assertEquals(List.of("Angular"), titles2);

        // usuario2 não consegue ler, renomear nem excluir a conversa do usuario1
        assertThrows(NotFoundException.class, () -> conversations.getMine(java.getId()));
        assertThrows(NotFoundException.class, () -> conversations.messagesOf(java.getId()));
        assertThrows(NotFoundException.class, () -> conversations.rename(java.getId(), "Invadido"));
        assertThrows(NotFoundException.class, () -> conversations.delete(java.getId()));
        auth.logout();

        auth.login("usuario1@teste.com", "senha1234");
        List<String> titles1 = conversations.listMine().stream().map(Conversation::getTitle).sorted().toList();
        assertEquals(List.of("Java", "JavaFX"), titles1);
    }

    @Test
    void criarRenomearEExcluir() {
        auth.login("usuario1@teste.com", "senha1234");
        Conversation created = conversations.create();
        assertEquals(Conversation.DEFAULT_TITLE, created.getTitle());
        assertNotNull(created.getCreatedAt());
        assertEquals(created.getUserId(), ctx.app.session().requireUserId());

        Conversation renamed = conversations.rename(created.getId(), "Estudando JavaFX");
        assertEquals("Estudando JavaFX", renamed.getTitle());
        assertEquals("Estudando JavaFX", ctx.conversations.data.get(created.getId()).getTitle());

        conversations.delete(created.getId());
        assertTrue(conversations.listMine().isEmpty());
        assertThrows(NotFoundException.class, () -> conversations.delete(created.getId()));
    }

    @Test
    void conversaInexistenteOuIdInvalido() {
        auth.login("usuario1@teste.com", "senha1234");
        assertThrows(NotFoundException.class, () -> conversations.getMine("000000000000000000000000"));
        assertThrows(NotFoundException.class, () -> conversations.getMine("id-invalido"));
    }
}
