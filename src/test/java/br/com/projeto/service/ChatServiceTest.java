package br.com.projeto.service;

import br.com.projeto.exception.AiServiceException;
import br.com.projeto.exception.ValidationException;
import br.com.projeto.model.ChatReply;
import br.com.projeto.model.Conversation;
import br.com.projeto.model.Message;
import br.com.projeto.model.MessageRole;
import br.com.projeto.support.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatServiceTest {

    private final TestContext ctx = new TestContext();
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        ctx.app.knowledgeBaseService().seedIfEmpty();
        ctx.app.authenticationService().register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        ctx.app.authenticationService().login("usuario1@teste.com", "senha1234");
        conversation = ctx.app.conversationService().create();
    }

    @Test
    void enviaPerguntaPersisteHistoricoEUsaContextoDoRag() {
        ChatReply reply = ctx.app.chatService().send(conversation.getId(), "Qual é a política de home office da empresa?");

        assertEquals("Resposta de teste.", reply.assistantMessage().getContent());
        assertTrue(reply.assistantMessage().getSources().contains("Política de home office da empresa"));

        String systemPrompt = ctx.ai.lastPrompt().get(0).content();
        assertEquals(MessageRole.SYSTEM, ctx.ai.lastPrompt().get(0).role());
        assertTrue(systemPrompt.contains("3 dias por semana"), "o contexto recuperado deve ir para a IA");

        List<Message> history = ctx.app.conversationService().messagesOf(conversation.getId());
        assertEquals(2, history.size());
        assertEquals(MessageRole.USER, history.get(0).getRole());
        assertEquals(MessageRole.ASSISTANT, history.get(1).getRole());
        assertEquals(ctx.app.session().requireUserId(), history.get(0).getUserId());
    }

    @Test
    void primeiraPerguntaViraTituloDaConversa() {
        ChatReply reply = ctx.app.chatService().send(conversation.getId(), "Explique encapsulamento em Java");
        assertEquals("Explique encapsulamento em Java", reply.conversation().getTitle());
    }

    @Test
    void historicoAnteriorEhEnviadoComoMemoria() {
        ctx.app.chatService().send(conversation.getId(), "Explique encapsulamento em Java");
        ctx.app.chatService().send(conversation.getId(), "E herança?");
        assertEquals(4, ctx.ai.lastPrompt().size()); // system + user + assistant + nova pergunta
    }

    @Test
    void erroDaApiNaoDeixaMensagemPelaMetade() {
        ctx.ai.fail = true;
        assertThrows(AiServiceException.class, () -> ctx.app.chatService().send(conversation.getId(), "Olá"));
        assertTrue(ctx.messages.data.isEmpty());
    }

    @Test
    void mensagemVaziaEhRecusada() {
        assertThrows(ValidationException.class, () -> ctx.app.chatService().send(conversation.getId(), "   "));
        assertTrue(ctx.ai.calls.isEmpty());
    }

    @Test
    void tituloAutomaticoEhTruncado() {
        String title = ChatService.autoTitle("Como configurar a conexão com o MongoDB Atlas usando variáveis de ambiente?");
        assertTrue(title.length() <= 41);
        assertTrue(title.endsWith("…"));
    }
}
