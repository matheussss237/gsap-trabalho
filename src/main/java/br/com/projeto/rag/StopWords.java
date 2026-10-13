package br.com.projeto.rag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Etapa 3 do RAG — lista de stop words (palavras irrelevantes para a busca).
 * Artigos, preposições, pronomes e verbos de pedido ("explique", "fale") aparecem em
 * praticamente qualquer pergunta e não ajudam a identificar o assunto, por isso são removidos.
 * A lista fica em resources/rag/stopwords-pt.txt e é normalizada como o restante do texto.
 */
public final class StopWords {

    private static final String RESOURCE = "/rag/stopwords-pt.txt";
    private static final Set<String> WORDS = load();

    private StopWords() {
    }

    public static boolean isStopWord(String token) {
        return WORDS.contains(token);
    }

    public static Set<String> all() {
        return WORDS;
    }

    private static Set<String> load() {
        InputStream in = StopWords.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IllegalStateException("Lista de stop words não encontrada: " + RESOURCE);
        }
        Set<String> words = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = TextNormalizer.normalize(line);
                if (!word.isEmpty() && !line.startsWith("#")) {
                    words.add(word);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Collections.unmodifiableSet(words);
    }
}
