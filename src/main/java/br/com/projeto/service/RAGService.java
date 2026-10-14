package br.com.projeto.service;

import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.rag.TextProcessor;
import br.com.projeto.repository.DocumentRepository;
import br.com.projeto.util.AppException;
import br.com.projeto.util.Security;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * RAG por palavras-chave + manutenção da base de conhecimento.
 *
 * Pergunta → normalização → tokenização → remoção de stop words → palavras-chave
 * → busca no MongoDB (documents.keywords) → pontuação → contexto enviado à Groq.
 */
public class RAGService {

    public static final int MAX_DOCUMENTS = 3;
    private static final int KEYWORD_WEIGHT = 3, TITLE_WEIGHT = 2, CONTENT_WEIGHT = 1;

    /** Documento recuperado, sua pontuação e as palavras da pergunta que ele contém. */
    public record ScoredDocument(KnowledgeDocument document, int score, List<String> matchedTerms) {
    }

    /** Palavras-chave da pergunta + documentos relevantes (já ordenados). */
    public record Result(List<String> keywords, List<ScoredDocument> documents) {

        public boolean hasContext() {
            return !documents.isEmpty();
        }

        public List<String> sourceTitles() {
            return documents.stream().map(d -> d.document().getTitle()).toList();
        }

        /** Trecho com os documentos que vai na mensagem "system" para a IA. */
        public String toPromptContext() {
            StringBuilder sb = new StringBuilder();
            int index = 1;
            for (ScoredDocument scored : documents) {
                String content = scored.document().getContent();
                if (content.length() > 2500) {
                    content = content.substring(0, 2500) + "...";
                }
                sb.append("[Documento ").append(index++).append("] ").append(scored.document().getTitle())
                        .append('\n').append(content).append("\n\n");
            }
            return sb.toString().trim();
        }
    }

    private final DocumentRepository documentRepository;
    private final AuthenticationService auth;

    public RAGService(DocumentRepository documentRepository, AuthenticationService auth) {
        this.documentRepository = documentRepository;
        this.auth = auth;
    }

    // ---------- recuperação ----------

    public Result retrieve(String question) {
        List<String> keywords = TextProcessor.keywords(question);
        if (keywords.isEmpty()) {
            return new Result(keywords, List.of());
        }
        List<KnowledgeDocument> candidates = documentRepository.findByAnyKeyword(keywords, auth.requireUserId());
        return new Result(keywords, rank(candidates, keywords, MAX_DOCUMENTS));
    }

    /**
     * Cada termo da pergunta vale 3 pontos se for palavra-chave do documento,
     * 2 se aparecer no título e 1 se aparecer no conteúdo. Pontuação zero é descartada.
     */
    public static List<ScoredDocument> rank(Collection<KnowledgeDocument> documents, Collection<String> terms, int limit) {
        return documents.stream()
                .map(doc -> score(doc, terms))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(ScoredDocument::score).reversed()
                        .thenComparing(s -> s.document().getTitle()))
                .limit(limit)
                .toList();
    }

    static ScoredDocument score(KnowledgeDocument doc, Collection<String> terms) {
        Set<String> keywords = TextProcessor.keywordsOf(doc.getKeywords());
        Set<String> title = new HashSet<>(TextProcessor.keywords(doc.getTitle()));
        Set<String> content = new HashSet<>(TextProcessor.keywords(doc.getContent()));
        int score = 0;
        List<String> matched = new ArrayList<>();
        for (String term : new LinkedHashSet<>(terms)) {
            int points = (keywords.contains(term) ? KEYWORD_WEIGHT : 0)
                    + (title.contains(term) ? TITLE_WEIGHT : 0)
                    + (content.contains(term) ? CONTENT_WEIGHT : 0);
            if (points > 0) {
                matched.add(term);
                score += points;
            }
        }
        matched.sort(Comparator.naturalOrder());
        return new ScoredDocument(doc, score, matched);
    }

    /** Mensagem de sistema: personalidade da Lumi, nome do usuário e contexto recuperado. */
    public String buildSystemPrompt(Result retrieval, String userFirstName) {
        StringBuilder prompt = new StringBuilder("""
                Você é a Lumi, a assistente de IA pessoal de %s.
                Chame o usuário pelo nome quando fizer sentido, responda sempre em português do Brasil,
                de forma clara, objetiva e didática, como uma colega que explica bem.
                Use Markdown simples (títulos, listas, **negrito** e blocos de código) quando ajudar a leitura.
                """.formatted(userFirstName.isBlank() ? "um colaborador da empresa" : userFirstName));
        if (retrieval.hasContext()) {
            prompt.append("""

                    A base de conhecimento interna retornou os documentos abaixo. Use-os como fonte principal;
                    se a resposta não estiver neles, complemente com seu conhecimento geral e deixe claro
                    o que veio de fora da base.

                    """).append(retrieval.toPromptContext());
        } else {
            prompt.append("\nNenhum documento da base de conhecimento interna é relevante para esta pergunta; "
                    + "responda com seu conhecimento geral.");
        }
        return prompt.toString();
    }

    // ---------- base de conhecimento ----------

    public List<KnowledgeDocument> listDocuments() {
        return documentRepository.findVisibleTo(auth.requireUserId());
    }

    /** Cadastra um documento do usuário logado; as palavras-chave são normalizadas e complementadas. */
    public KnowledgeDocument addDocument(String title, String content, String keywordsCsv) {
        String validTitle = Security.title(title);
        String validContent = Security.required(content, "Conteúdo");
        if (validContent.length() > 20_000) {
            throw new AppException("O conteúdo deve ter no máximo 20.000 caracteres.");
        }
        List<String> manual = keywordsCsv == null ? List.of() : Arrays.asList(keywordsCsv.split("[,;]"));
        return documentRepository.save(new KnowledgeDocument(auth.requireUserId(), validTitle, validContent,
                indexKeywords(validTitle, validContent, manual)));
    }

    public void deleteDocument(String documentId) {
        if (!documentRepository.deleteByIdAndOwner(documentId, auth.requireUserId())) {
            throw new AppException("Só é possível excluir documentos cadastrados por você.");
        }
    }

    /** Na primeira execução (base vazia) cadastra os documentos de resources/rag/seed-documents.json. */
    public int seedIfEmpty() {
        if (documentRepository.countPublic() > 0) {
            return 0;
        }
        InputStream in = getClass().getResourceAsStream("/rag/seed-documents.json");
        if (in == null) {
            return 0;
        }
        int count = 0;
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            for (JsonElement element : JsonParser.parseReader(reader).getAsJsonArray()) {
                JsonObject obj = element.getAsJsonObject();
                List<String> manual = new ArrayList<>();
                JsonArray keywords = obj.getAsJsonArray("keywords");
                keywords.forEach(k -> manual.add(k.getAsString()));
                String title = obj.get("title").getAsString();
                String content = obj.get("content").getAsString();
                documentRepository.save(new KnowledgeDocument(null, title, content,
                        indexKeywords(title, content, manual)));
                count++;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler a base de conhecimento inicial", e);
        }
        return count;
    }

    /** Palavras-chave informadas + palavras do título + as 40 mais frequentes do conteúdo. */
    static List<String> indexKeywords(String title, String content, List<String> manual) {
        Set<String> keywords = new LinkedHashSet<>(TextProcessor.keywordsOf(manual));
        keywords.addAll(TextProcessor.keywords(title));
        keywords.addAll(TextProcessor.topKeywords(content, 40));
        return new ArrayList<>(keywords);
    }
}
