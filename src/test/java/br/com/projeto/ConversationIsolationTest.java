package br.com.projeto;

import br.com.projeto.model.Conversation;
import br.com.projeto.util.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Conversas: criar, renomear, excluir e o isolamento usuario1 ≠ usuario2. */
class ConversationIsolationTest {

    private final TestSupport app = new TestSupport();

    @BeforeEach
    void criaUsuarios() {
        app.auth.register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        app.auth.register("Usuário Dois", "usuario2@teste.com", "senha1234", "senha1234");
    }

    @Test
    void cadaUsuarioVeApenasSuasConversas() {
        app.auth.login("usuario1@teste.com", "senha1234");
        Conversation java = app.conversations.rename(app.conversations.create().getId(), "Java");
        app.conversations.rename(app.conversations.create().getId(), "JavaFX");
        app.auth.logout();

        app.auth.login("usuario2@teste.com", "senha1234");
        app.conversations.rename(app.conversations.create().getId(), "Angular");
        assertEquals(List.of("Angular"), app.conversations.listMine().stream().map(Conversation::getTitle).toList());
        // usuario2 não consegue ler, renomear nem excluir a conversa do usuario1
        assertThrows(AppException.class, () -> app.conversations.getMine(java.getId()));
        assertThrows(AppException.class, () -> app.conversations.messagesOf(java.getId()));
        assertThrows(AppException.class, () -> app.conversations.rename(java.getId(), "Invadido"));
        assertThrows(AppException.class, () -> app.conversations.delete(java.getId()));
        app.auth.logout();

        app.auth.login("usuario1@teste.com", "senha1234");
        assertEquals(List.of("Java", "JavaFX"),
                app.conversations.listMine().stream().map(Conversation::getTitle).sorted().toList());
    }

    @Test
    void criarRenomearEExcluir() {
        app.auth.login("usuario1@teste.com", "senha1234");
        Conversation created = app.conversations.create();
        assertEquals(Conversation.DEFAULT_TITLE, created.getTitle());
        assertEquals(app.auth.requireUserId(), created.getUserId());
        assertNotNull(created.getCreatedAt());

        assertEquals("Estudando JavaFX", app.conversations.rename(created.getId(), "Estudando JavaFX").getTitle());
        assertEquals("Estudando JavaFX", app.conversationRepo.data.get(created.getId()).getTitle());

        app.conversations.delete(created.getId());
        assertTrue(app.conversations.listMine().isEmpty());
        assertThrows(AppException.class, () -> app.conversations.delete(created.getId()));
    }

    @Test
    void conversaInexistenteOuIdInvalido() {
        app.auth.login("usuario1@teste.com", "senha1234");
        assertThrows(AppException.class, () -> app.conversations.getMine("000000000000000000000000"));
        assertThrows(AppException.class, () -> app.conversations.getMine("id-invalido"));
    }
}
