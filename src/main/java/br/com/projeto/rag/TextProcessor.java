package br.com.projeto.rag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Processamento de texto do RAG:
 *
 *   "Explique o Encapsulamento em JAVA!"
 *   1. normalização  → "explique o encapsulamento em java"
 *   2. tokenização   → [explique, o, encapsulamento, em, java]
 *   3. stop words    → remove [explique, o, em]
 *   4. palavras-chave → [encapsulamento, java]
 *
 * Também aplica uma redução simples de plural ("conversas" → "conversa", "padrões" → "padrao").
 */
public final class TextProcessor {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    // mantém letras, dígitos e os símbolos de nomes como "c++" e "c#"
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9+#]+");
    private static final Set<String> STOP_WORDS = loadStopWords();

    private TextProcessor() {
    }

    /** Etapa 1: minúsculas, sem acentos e sem pontuação. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String noAccents = DIACRITICS.matcher(Normalizer.normalize(lower, Normalizer.Form.NFD)).replaceAll("");
        return NON_WORD.matcher(noAccents).replaceAll(" ").trim();
    }

    /** Etapa 2: separa o texto normalizado em palavras. */
    public static List<String> tokenize(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalizedText.trim().split("\\s+"))
                .map(token -> token.replaceAll("^[+#]+", "")) // "+" e "#" só valem no fim (c++, c#)
                .filter(token -> !token.isEmpty())
                .toList();
    }

    /** Etapa 3: palavras irrelevantes para a busca (lista em resources/rag/stopwords-pt.txt). */
    public static boolean isStopWord(String token) {
        return STOP_WORDS.contains(token);
    }

    /** Etapas 1 a 4: palavras-chave distintas, na ordem em que aparecem. */
    public static List<String> keywords(String text) {
        return new ArrayList<>(new LinkedHashSet<>(relevantTokens(text)));
    }

    /** As {@code limit} palavras mais frequentes do texto (para indexar documentos). */
    public static List<String> topKeywords(String text, int limit) {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        relevantTokens(text).forEach(token -> frequency.merge(token, 1, Integer::sum));
        return frequency.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Normaliza palavras-chave digitadas manualmente (ex.: "Banco de Dados", "MongoDB"). */
    public static Set<String> keywordsOf(Collection<String> phrases) {
        Set<String> result = new LinkedHashSet<>();
        phrases.forEach(phrase -> result.addAll(keywords(phrase)));
        return result;
    }

    private static List<String> relevantTokens(String text) {
        List<String> result = new ArrayList<>();
        for (String token : tokenize(normalize(text))) {
            if (token.length() < 2 || isStopWord(token) || token.chars().allMatch(Character::isDigit)) {
                continue;
            }
            String stem = stem(token);
            if (!isStopWord(stem)) {
                result.add(stem);
            }
        }
        return result;
    }

    /** Redução de plural bem simples para o português. */
    static String stem(String word) {
        if (word.length() <= 3 || word.endsWith("ss") || word.endsWith("us") || word.endsWith("is")) {
            return word;
        }
        if (word.endsWith("oes") || word.endsWith("aes")) {
            return word.substring(0, word.length() - 3) + "ao";
        }
        if (word.endsWith("res") || word.endsWith("zes")) {
            return word.substring(0, word.length() - 2);
        }
        return word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
    }

    private static Set<String> loadStopWords() {
        InputStream in = TextProcessor.class.getResourceAsStream("/rag/stopwords-pt.txt");
        if (in == null) {
            throw new IllegalStateException("Lista de stop words não encontrada.");
        }
        Set<String> words = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && !normalize(line).isEmpty()) {
                    words.add(normalize(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Set.copyOf(words);
    }
}
