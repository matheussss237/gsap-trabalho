package br.com.projeto.rag;

import br.com.projeto.model.KnowledgeDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Etapa 5 do RAG — classificação dos documentos encontrados.
 * Cada termo da pergunta vale: 3 pontos se for palavra-chave do documento,
 * 2 se aparecer no título e 1 se aparecer no conteúdo.
 */
public final class DocumentScorer {

    static final int KEYWORD_WEIGHT = 3;
    static final int TITLE_WEIGHT = 2;
    static final int CONTENT_WEIGHT = 1;

    private DocumentScorer() {
    }

    public static ScoredDocument score(KnowledgeDocument document, Collection<String> queryTerms) {
        Set<String> keywords = new HashSet<>(KeywordExtractor.normalizeAll(document.getKeywords()));
        Set<String> titleTerms = new HashSet<>(KeywordExtractor.extract(document.getTitle()));
        Set<String> contentTerms = new HashSet<>(KeywordExtractor.extract(document.getContent()));

        int score = 0;
        List<String> matched = new ArrayList<>();
        for (String term : new HashSet<>(queryTerms)) {
            int termScore = 0;
            if (keywords.contains(term)) termScore += KEYWORD_WEIGHT;
            if (titleTerms.contains(term)) termScore += TITLE_WEIGHT;
            if (contentTerms.contains(term)) termScore += CONTENT_WEIGHT;
            if (termScore > 0) {
                matched.add(term);
                score += termScore;
            }
        }
        matched.sort(Comparator.naturalOrder());
        return new ScoredDocument(document, score, matched);
    }

    /** Os {@code limit} documentos de maior pontuação (pontuação zero é descartada). */
    public static List<ScoredDocument> rank(Collection<KnowledgeDocument> documents,
                                            Collection<String> queryTerms, int limit) {
        return documents.stream()
                .map(doc -> score(doc, queryTerms))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(ScoredDocument::score).reversed()
                        .thenComparing(s -> s.document().getTitle()))
                .limit(limit)
                .toList();
    }
}
