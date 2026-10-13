package br.com.projeto.rag;

import br.com.projeto.model.KnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentScorerTest {

    private final KnowledgeDocument java = doc("Encapsulamento em Java", "Atributos private e getters.",
            List.of("encapsulamento", "java"));
    private final KnowledgeDocument mongo = doc("MongoDB Atlas", "Banco NoSQL com coleções. Pode ser usado com Java.",
            List.of("mongodb", "atlas"));

    @Test
    void documentoComPalavraChaveETituloPontuaMais() {
        List<ScoredDocument> ranked = DocumentScorer.rank(List.of(mongo, java), List.of("encapsulamento", "java"), 3);
        assertEquals("Encapsulamento em Java", ranked.get(0).document().getTitle());
        assertTrue(ranked.get(0).score() > ranked.get(1).score());
        assertEquals(List.of("encapsulamento", "java"), ranked.get(0).matchedTerms());
    }

    @Test
    void documentoSemCorrespondenciaEDescartado() {
        assertTrue(DocumentScorer.rank(List.of(java, mongo), List.of("angular"), 3).isEmpty());
    }

    @Test
    void respeitaLimite() {
        assertEquals(1, DocumentScorer.rank(List.of(java, mongo), List.of("java"), 1).size());
    }

    private static KnowledgeDocument doc(String title, String content, List<String> keywords) {
        KnowledgeDocument d = new KnowledgeDocument(null, title, content, keywords);
        d.setId(title);
        return d;
    }
}
