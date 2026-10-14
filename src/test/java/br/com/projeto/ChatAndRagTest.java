package br.com.projeto;

import br.com.projeto.model.Conversation;
import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.model.Message;
import br.com.projeto.rag.TextProcessor;
import br.com.projeto.service.ChatService;
import br.com.projeto.service.RAGService;
import br.com.projeto.util.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Envio de mensagens para a IA, histórico e o mecanismo de RAG com stop words. */
class ChatAndRagTest {

    private final TestSupport app = new TestSupport();
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        assertTrue(app.rag.seedIfEmpty() > 0);
        app.auth.register("Matheus Henrique", "usuario1@teste.com", "senha1234", "senha1234");
        app.auth.register("Usuário Dois", "usuario2@teste.com", "senha1234", "senha1234");
        app.auth.login("usuario1@teste.com", "senha1234");
        conversation = app.conversations.create();
    }

    // ---------- chat ----------

    @Test
    void enviaPerguntaPersisteHistoricoEUsaContextoDoRag() {
        ChatService.Reply reply = app.chat.send(conversation.getId(), "Qual é a política de home office da empresa?");

        assertEquals("Resposta de teste.", reply.answer().getContent());
        assertTrue(reply.answer().getSources().contains("Política de home office da empresa"));
        String system = app.groq.lastPrompt().get(0).content();
        assertTrue(system.contains("3 dias por semana"), "o contexto recuperado deve ir para a IA");
        assertTrue(system.contains("Matheus"), "a Lumi é personalizada com o nome do usuário");

        List<Message> history = app.conversations.messagesOf(conversation.getId());
        assertEquals(List.of(Message.Role.USER, Message.Role.ASSISTANT), history.stream().map(Message::getRole).toList());
        assertEquals(app.auth.requireUserId(), history.get(0).getUserId());
        assertEquals("Qual é a política de home office da…", reply.conversation().getTitle()); // título automático
    }

    @Test
    void historicoAnteriorEhEnviadoComoMemoria() {
        app.chat.send(conversation.getId(), "Explique encapsulamento em Java");
        app.chat.send(conversation.getId(), "E herança?");
        assertEquals(4, app.groq.lastPrompt().size()); // system + pergunta + resposta + nova pergunta
    }

    @Test
    void erroDaApiNaoDeixaMensagemPelaMetade() {
        app.groq.fail = true;
        assertThrows(AppException.class, () -> app.chat.send(conversation.getId(), "Olá"));
        assertTrue(app.messageRepo.data.isEmpty());
    }

    @Test
    void mensagemVaziaEhRecusada() {
        assertThrows(AppException.class, () -> app.chat.send(conversation.getId(), "   "));
        assertTrue(app.groq.calls.isEmpty());
    }

    // ---------- RAG ----------

    @Test
    void normalizacaoTokenizacaoEStopWords() {
        assertEquals("politica de ferias", TextProcessor.normalize("Política   de   férias?"));
        assertEquals(List.of("como", "usar", "javafx"), TextProcessor.tokenize("como usar javafx"));
        for (String word : List.of("a", "o", "as", "os", "de", "da", "do", "em", "para", "por", "com", "uma", "um", "que")) {
            assertTrue(TextProcessor.isStopWord(word), word);
        }
        assertEquals(List.of("encapsulamento", "java"),
                TextProcessor.keywords("Explique o que é encapsulamento em Java, por favor"));
        assertTrue(TextProcessor.keywords("o que é isso para você?").isEmpty());
        assertEquals(TextProcessor.keywords("padrão"), TextProcessor.keywords("padrões"));
    }

    @Test
    void palavraChaveExistenteRecuperaDocumento() {
        RAGService.Result result = app.rag.retrieve("Explique o encapsulamento em Java");
        assertEquals("Encapsulamento em Java", result.documents().get(0).document().getTitle());
        assertTrue(result.documents().get(0).score() > 0);
    }

    @Test
    void palavraChaveInexistenteNaoRetornaContexto() {
        RAGService.Result result = app.rag.retrieve("Receita de bolo de cenoura");
        assertFalse(result.hasContext());
        assertTrue(app.rag.buildSystemPrompt(result, "Ana").contains("conhecimento geral"));
    }

    @Test
    void documentoCadastradoSoAjudaOProprioUsuario() {
        KnowledgeDocument doc = app.rag.addDocument("Política de férias",
                "Os colaboradores podem dividir as férias em até três períodos.", "férias, rh");
        assertTrue(doc.getKeywords().contains("feria"));
        assertEquals("Política de férias", app.rag.retrieve("como funcionam as férias?").documents().get(0)
                .document().getTitle());

        app.auth.logout();
        app.auth.login("usuario2@teste.com", "senha1234");
        assertFalse(app.rag.retrieve("como funcionam as férias?").hasContext());
        assertThrows(AppException.class, () -> app.rag.deleteDocument(doc.getId()));
    }
}
