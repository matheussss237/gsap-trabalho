package br.com.projeto.rag;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Etapa 1 do RAG — normalização.
 * "Explique o Encapsulamento em JAVA!" → "explique o encapsulamento em java"
 * Converte para minúsculas, remove acentos e troca pontuação por espaço.
 */
public final class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    // mantém letras, dígitos e os símbolos de nomes como "c++" e "c#"
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9+#]+");

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String withoutAccents = DIACRITICS.matcher(Normalizer.normalize(lower, Normalizer.Form.NFD)).replaceAll("");
        return NON_WORD.matcher(withoutAccents).replaceAll(" ").trim();
    }
}
