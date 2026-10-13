package br.com.projeto.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextProcessingTest {

    @Test
    void normalizaMinusculasAcentosEPontuacao() {
        assertEquals("explique o encapsulamento em java", TextNormalizer.normalize("Explique o Encapsulamento em JAVA!"));
        assertEquals("politica de ferias", TextNormalizer.normalize("Política   de   férias?"));
        assertEquals("c++ e c#", TextNormalizer.normalize("C++ e C#"));
        assertEquals("", TextNormalizer.normalize(null));
    }

    @Test
    void tokenizaPorEspacos() {
        assertEquals(List.of("como", "usar", "javafx"), Tokenizer.tokenize("como usar javafx"));
        assertTrue(Tokenizer.tokenize("   ").isEmpty());
    }

    @Test
    void listaDeStopWordsContemPalavrasExigidas() {
        for (String word : List.of("a", "o", "as", "os", "de", "da", "do", "em", "para", "por", "com", "uma", "um", "que")) {
            assertTrue(StopWords.isStopWord(word), "deveria ser stop word: " + word);
        }
        assertFalse(StopWords.isStopWord("java"));
        assertFalse(StopWords.isStopWord("mongodb"));
    }

    @Test
    void removeStopWordsEExtraiPalavrasRelevantes() {
        List<String> keywords = KeywordExtractor.extract("Explique o que é encapsulamento em Java, por favor");
        assertEquals(List.of("encapsulamento", "java"), keywords);
    }

    @Test
    void perguntaSoComStopWordsNaoGeraPalavrasChave() {
        assertTrue(KeywordExtractor.extract("o que é isso para você?").isEmpty());
    }

    @Test
    void reduzPluralParaEncontrarSingular() {
        assertEquals(KeywordExtractor.extract("conversa"), KeywordExtractor.extract("conversas"));
        assertEquals(KeywordExtractor.extract("padrão"), KeywordExtractor.extract("padrões"));
        assertEquals(List.of("classe"), KeywordExtractor.extract("classes"));
    }

    @Test
    void topKeywordsOrdenaPorFrequencia() {
        List<String> top = KeywordExtractor.topKeywords("java java java mongodb mongodb javafx", 2);
        assertEquals(List.of("java", "mongodb"), top);
    }
}
