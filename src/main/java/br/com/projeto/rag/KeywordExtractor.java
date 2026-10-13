package br.com.projeto.rag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Etapas 1 a 4 do RAG combinadas: normaliza, tokeniza, remove stop words e extrai
 * as palavras relevantes. Também aplica uma redução simples de plural
 * ("conversas" → "conversa", "padrões" → "padrao") para que singular e plural se encontrem.
 */
public final class KeywordExtractor {

    private static final int MIN_LENGTH = 2;

    private KeywordExtractor() {
    }

    /** Palavras-chave distintas, na ordem em que aparecem. */
    public static List<String> extract(String text) {
        return new ArrayList<>(new LinkedHashSet<>(relevantTokens(text)));
    }

    /** As {@code limit} palavras mais frequentes do texto (para indexar documentos). */
    public static List<String> topKeywords(String text, int limit) {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        for (String token : relevantTokens(text)) {
            frequency.merge(token, 1, Integer::sum);
        }
        return frequency.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Normaliza palavras-chave digitadas manualmente (ex.: "Banco de Dados, MongoDB"). */
    public static Set<String> normalizeAll(Collection<String> keywords) {
        Set<String> result = new LinkedHashSet<>();
        for (String keyword : keywords) {
            result.addAll(extract(keyword));
        }
        return result;
    }

    private static List<String> relevantTokens(String text) {
        List<String> result = new ArrayList<>();
        for (String token : Tokenizer.tokenize(TextNormalizer.normalize(text))) {
            if (token.length() < MIN_LENGTH || StopWords.isStopWord(token) || isNumber(token)) {
                continue;
            }
            String stem = stem(token);
            if (!StopWords.isStopWord(stem)) {
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
        if (word.endsWith("s")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    private static boolean isNumber(String token) {
        return token.chars().allMatch(Character::isDigit);
    }
}
