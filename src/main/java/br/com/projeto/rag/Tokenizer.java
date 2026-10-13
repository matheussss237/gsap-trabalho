package br.com.projeto.rag;

import java.util.Arrays;
import java.util.List;

/**
 * Etapa 2 do RAG — tokenização: separa o texto normalizado em palavras.
 * "explique o encapsulamento em java" → [explique, o, encapsulamento, em, java]
 */
public final class Tokenizer {

    private Tokenizer() {
    }

    public static List<String> tokenize(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalizedText.trim().split("\\s+"))
                .map(Tokenizer::trimSymbols)
                .filter(token -> !token.isEmpty())
                .toList();
    }

    /** "+" e "#" só fazem sentido no fim de palavras como c++ / c#. */
    private static String trimSymbols(String token) {
        int start = 0;
        while (start < token.length() && (token.charAt(start) == '+' || token.charAt(start) == '#')) {
            start++;
        }
        return token.substring(start);
    }
}
