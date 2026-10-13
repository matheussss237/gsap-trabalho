package br.com.projeto.service;

import br.com.projeto.exception.NotFoundException;
import br.com.projeto.exception.ValidationException;
import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.rag.KeywordExtractor;
import br.com.projeto.repository.DocumentRepository;
import br.com.projeto.util.Validator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Cadastro e manutenção dos documentos da base de conhecimento. */
public class KnowledgeBaseService {

    private static final int AUTO_KEYWORDS = 40;
    private static final int MAX_CONTENT = 20_000;
    private static final String SEED_RESOURCE = "/rag/seed-documents.json";

    private final DocumentRepository documentRepository;
    private final SessionManager session;

    public KnowledgeBaseService(DocumentRepository documentRepository, SessionManager session) {
        this.documentRepository = documentRepository;
        this.session = session;
    }

    public List<KnowledgeDocument> listVisible() {
        return documentRepository.findVisibleTo(session.requireUserId());
    }

    /** Cadastra um documento do usuário logado. As palavras-chave são normalizadas e complementadas. */
    public KnowledgeDocument add(String title, String content, String keywordsCsv) {
        String validTitle = Validator.title(title);
        String validContent = Validator.required(content, "Conteúdo");
        if (validContent.length() > MAX_CONTENT) {
            throw new ValidationException("O conteúdo deve ter no máximo " + MAX_CONTENT + " caracteres.");
        }
        List<String> manual = keywordsCsv == null ? List.of() : Arrays.asList(keywordsCsv.split("[,;]"));
        return documentRepository.save(new KnowledgeDocument(session.requireUserId(), validTitle, validContent,
                buildKeywords(validTitle, validContent, manual)));
    }

    public void delete(String documentId) {
        if (!documentRepository.deleteByIdAndOwner(documentId, session.requireUserId())) {
            throw new NotFoundException("Só é possível excluir documentos cadastrados por você.");
        }
    }

    /** Cadastra os documentos de exemplo na primeira execução (base vazia). */
    public int seedIfEmpty() {
        if (documentRepository.countPublic() > 0) {
            return 0;
        }
        InputStream in = getClass().getResourceAsStream(SEED_RESOURCE);
        if (in == null) {
            return 0;
        }
        int count = 0;
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                List<String> manual = new ArrayList<>();
                obj.getAsJsonArray("keywords").forEach(k -> manual.add(k.getAsString()));
                String title = obj.get("title").getAsString();
                String content = obj.get("content").getAsString();
                documentRepository.save(new KnowledgeDocument(null, title, content,
                        buildKeywords(title, content, manual)));
                count++;
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Falha ao ler " + SEED_RESOURCE, e);
        }
        return count;
    }

    static List<String> buildKeywords(String title, String content, List<String> manual) {
        Set<String> keywords = new LinkedHashSet<>(KeywordExtractor.normalizeAll(manual));
        keywords.addAll(KeywordExtractor.extract(title));
        keywords.addAll(KeywordExtractor.topKeywords(content, AUTO_KEYWORDS));
        return new ArrayList<>(keywords);
    }
}
