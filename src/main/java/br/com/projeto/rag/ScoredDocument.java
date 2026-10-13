package br.com.projeto.rag;

import br.com.projeto.model.KnowledgeDocument;

import java.util.List;

/** Documento recuperado com sua pontuação e as palavras da pergunta que ele contém. */
public record ScoredDocument(KnowledgeDocument document, int score, List<String> matchedTerms) {
}
