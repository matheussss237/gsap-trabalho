package br.com.projeto.rag;

import java.util.List;

/**
 * Resultado da recuperação: as palavras-chave extraídas da pergunta e os documentos
 * relevantes. {@link #toPromptContext()} monta o trecho enviado à Groq.
 */
public record RetrievalResult(List<String> queryKeywords, List<ScoredDocument> documents) {

    private static final int MAX_CHARS_PER_DOCUMENT = 2500;

    public boolean hasContext() {
        return !documents.isEmpty();
    }

    public List<String> sourceTitles() {
        return documents.stream().map(d -> d.document().getTitle()).toList();
    }

    public String toPromptContext() {
        if (!hasContext()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (ScoredDocument scored : documents) {
            String content = scored.document().getContent();
            if (content.length() > MAX_CHARS_PER_DOCUMENT) {
                content = content.substring(0, MAX_CHARS_PER_DOCUMENT) + "...";
            }
            sb.append("[Documento ").append(index++).append("] ").append(scored.document().getTitle()).append('\n')
              .append(content).append("\n\n");
        }
        return sb.toString().trim();
    }
}
