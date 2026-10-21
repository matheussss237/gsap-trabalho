package com.matheushenrique.lumi.business;

import com.matheushenrique.lumi.dto.RespostaChatDTO;
import com.matheushenrique.lumi.dto.ResultadoRagDTO;
import com.matheushenrique.lumi.exceptions.RegraNegocioException;
import com.matheushenrique.lumi.exceptions.ResourceNotFoundException;
import com.matheushenrique.lumi.infrastructure.entitys.Conversa;
import com.matheushenrique.lumi.infrastructure.entitys.Documento;
import com.matheushenrique.lumi.infrastructure.entitys.Mensagem;
import com.matheushenrique.lumi.rag.ProcessadorTexto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// envio de mensagens para a IA + RAG com stop words
class ChatERagTest extends TesteBase {

    private Conversa conversa;

    @BeforeEach
    void preparar() {
        assertTrue(ragService.cadastrarDocumentosIniciais() > 0);
        assertEquals(0, ragService.cadastrarDocumentosIniciais(), "a base inicial só é cadastrada uma vez");
        cadastrar("Matheus Henrique", "usuario1@teste.com");
        cadastrar("Usuário Dois", "usuario2@teste.com");
        entrar("usuario1@teste.com");
        conversa = conversaService.criarConversa();
    }

    @Test
    void enviaPerguntaSalvaHistoricoEUsaORag() {
        RespostaChatDTO resposta = chatService.enviarMensagem(conversa.getId(), "Qual é a política de home office da empresa?");

        assertEquals("Resposta de teste.", resposta.resposta().getConteudo());
        assertTrue(resposta.resposta().getFontes().contains("Política de home office da empresa"));
        String instrucoes = groq.ultimaPergunta().get(0).getConteudo();
        assertTrue(instrucoes.contains("3 dias por semana"), "o documento encontrado vai junto para a IA");
        assertTrue(instrucoes.contains("Matheus"), "a Lumi recebe o nome do usuário");

        List<Mensagem> historico = conversaService.listarMensagens(conversa.getId());
        assertEquals(List.of(Mensagem.Autor.USUARIO, Mensagem.Autor.ASSISTENTE),
                historico.stream().map(Mensagem::getAutor).toList());
        assertEquals("Qual é a política de home office da…", resposta.conversa().getTitulo()); // título automático
    }

    @Test
    void mensagensAnterioresVaoComoMemoria() {
        chatService.enviarMensagem(conversa.getId(), "Explique encapsulamento em Java");
        chatService.enviarMensagem(conversa.getId(), "E herança?");
        assertEquals(4, groq.ultimaPergunta().size()); // instruções + pergunta + resposta + pergunta nova
    }

    @Test
    void erroDaApiNaoSalvaNada() {
        groq.falhar = true;
        assertThrows(RegraNegocioException.class, () -> chatService.enviarMensagem(conversa.getId(), "Olá"));
        assertTrue(conversaService.listarMensagens(conversa.getId()).isEmpty());
    }

    @Test
    void mensagemVazia() {
        assertThrows(RegraNegocioException.class, () -> chatService.enviarMensagem(conversa.getId(), "   "));
        assertTrue(groq.perguntas.isEmpty());
    }

    @Test
    void removeStopWordsEExtraiPalavrasChave() {
        assertEquals("politica de ferias", ProcessadorTexto.normalizar("Política   de   férias?"));
        for (String palavra : List.of("a", "o", "as", "os", "de", "da", "do", "em", "para", "por", "com", "uma", "um", "que")) {
            assertTrue(ProcessadorTexto.isStopWord(palavra), palavra);
        }
        assertEquals(List.of("encapsulamento", "java"),
                ProcessadorTexto.extrairPalavrasChave("Explique o que é encapsulamento em Java, por favor"));
        assertTrue(ProcessadorTexto.extrairPalavrasChave("o que é isso para você?").isEmpty());
        assertEquals(ProcessadorTexto.extrairPalavrasChave("padrão"), ProcessadorTexto.extrairPalavrasChave("padrões"));
    }

    @Test
    void palavraChaveExistente() {
        ResultadoRagDTO resultado = ragService.buscarContexto("Explique o encapsulamento em Java");
        assertEquals("Encapsulamento em Java", resultado.documentos().get(0).documento().getTitulo());
    }

    @Test
    void palavraChaveInexistente() {
        ResultadoRagDTO resultado = ragService.buscarContexto("Receita de bolo de cenoura");
        assertFalse(resultado.temContexto());
        assertTrue(ragService.montarInstrucoes(resultado, "Ana").contains("conhecimento geral"));
    }

    @Test
    void documentoNovoSoServeParaODono() {
        Documento documento = ragService.cadastrarDocumento("Política de férias",
                "Os colaboradores podem dividir as férias em até três períodos.", "férias, rh");
        assertTrue(documento.getPalavrasChave().contains("feria"));
        assertEquals("Política de férias",
                ragService.buscarContexto("como funcionam as férias?").documentos().get(0).documento().getTitulo());

        usuarioService.fazerLogout();
        entrar("usuario2@teste.com");
        assertFalse(ragService.buscarContexto("como funcionam as férias?").temContexto());
        assertThrows(ResourceNotFoundException.class, () -> ragService.excluirDocumento(documento.getId()));
    }
}
