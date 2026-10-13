package br.com.projeto.service;

import br.com.projeto.exception.NotFoundException;
import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.rag.RetrievalResult;
import br.com.projeto.support.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RagServiceTest {

    private final TestContext ctx = new TestContext();

    @BeforeEach
    void setUp() {
        assertTrue(ctx.app.knowledgeBaseService().seedIfEmpty() > 0);
        assertEquals(0, ctx.app.knowledgeBaseService().seedIfEmpty(), "a semente só é aplicada uma vez");
        ctx.app.authenticationService().register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
        ctx.app.authenticationService().register("Usuário Dois", "usuario2@teste.com", "senha1234", "senha1234");
        ctx.app.authenticationService().login("usuario1@teste.com", "senha1234");
    }

    @Test
    void palavraChaveExistenteRecuperaDocumento() {
        RetrievalResult result = ctx.app.ragService().retrieve("Explique o encapsulamento em Java");
        assertTrue(result.hasContext());
        assertEquals("Encapsulamento em Java", result.documents().get(0).document().getTitle());
        assertFalse(result.queryKeywords().contains("o"));
        assertFalse(result.queryKeywords().contains("em"));
    }

    @Test
    void palavraChaveInexistenteNaoRetornaContexto() {
        RetrievalResult result = ctx.app.ragService().retrieve("Receita de bolo de cenoura");
        assertFalse(result.hasContext());
        assertTrue(ctx.app.ragService().buildSystemPrompt(result).contains("conhecimento geral"));
    }

    @Test
    void documentoCadastradoPassaASerEncontradoApenasPeloDono() {
        KnowledgeDocument doc = ctx.app.knowledgeBaseService().add("Política de férias",
                "Os colaboradores podem dividir as férias em até três períodos.", "férias, rh");
        assertTrue(doc.getKeywords().contains("feria"));
        assertEquals("Política de férias",
                ctx.app.ragService().retrieve("como funcionam as férias?").documents().get(0).document().getTitle());

        ctx.app.authenticationService().logout();
        ctx.app.authenticationService().login("usuario2@teste.com", "senha1234");
        assertFalse(ctx.app.ragService().retrieve("como funcionam as férias?").hasContext());
        assertThrows(NotFoundException.class, () -> ctx.app.knowledgeBaseService().delete(doc.getId()));
    }
}
