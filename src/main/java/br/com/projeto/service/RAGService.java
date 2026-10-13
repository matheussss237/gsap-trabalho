package br.com.projeto.service;

import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.rag.DocumentScorer;
import br.com.projeto.rag.KeywordExtractor;
import br.com.projeto.rag.RetrievalResult;
import br.com.projeto.rag.ScoredDocument;
import br.com.projeto.repository.DocumentRepository;

import java.util.List;

/**
 * Recuperação de informações por palavras-chave (primeira versão do RAG).
 *
 * Pergunta → normalização → tokenização → remoção de stop words → palavras relevantes
 * → busca no MongoDB (documents.keywords) → pontuação → contexto para a Groq.
 */
public class RAGService {

    public static final int MAX_DOCUMENTS = 3;

    private final DocumentRepository documentRepository;
    private final SessionManager session;

    public RAGService(DocumentRepository documentRepository, SessionManager session) {
        this.documentRepository = documentRepository;
        this.session = session;
    }

    public RetrievalResult retrieve(String question) {
        List<String> keywords = KeywordExtractor.extract(question);
        if (keywords.isEmpty()) {
            return new RetrievalResult(keywords, List.of());
        }
        List<KnowledgeDocument> candidates = documentRepository.findByAnyKeyword(keywords, session.requireUserId());
        List<ScoredDocument> ranked = DocumentScorer.rank(candidates, keywords, MAX_DOCUMENTS);
        return new RetrievalResult(keywords, ranked);
    }

    /** Monta a mensagem de sistema com as instruções e o contexto recuperado. */
    public String buildSystemPrompt(RetrievalResult retrieval) {
        StringBuilder prompt = new StringBuilder("""
                Você é o Nexus, um assistente de IA de uma empresa de tecnologia.
                Responda sempre em português do Brasil, de forma clara, objetiva e didática.
                Use Markdown simples (títulos, listas, **negrito** e blocos de código) quando ajudar a leitura.
                """);
        if (retrieval.hasContext()) {
            prompt.append("""

                    A base de conhecimento interna retornou os documentos abaixo.
                    Use-os como fonte principal; se a resposta não estiver neles, complemente com seu
                    conhecimento geral e deixe claro o que veio de fora da base.

                    """).append(retrieval.toPromptContext());
        } else {
            prompt.append("\nNenhum documento da base de conhecimento interna é relevante para esta pergunta; "
                    + "responda com seu conhecimento geral.");
        }
        return prompt.toString();
    }
}
