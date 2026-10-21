package com.matheushenrique.lumi.rag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

/*
 * Prepara o texto para o RAG:
 *   "Explique o Encapsulamento em JAVA!"
 *   1. normalizar   -> "explique o encapsulamento em java"
 *   2. tokenizar    -> [explique, o, encapsulamento, em, java]
 *   3. stop words   -> tira [explique, o, em]
 *   4. palavras-chave -> [encapsulamento, java]
 */
public class ProcessadorTexto {

    private static final Set<String> STOP_WORDS = carregarStopWords();

    private ProcessadorTexto() {
    }

    // 1. minúsculas, sem acento e sem pontuação (mantém c++ e c#)
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.replaceAll("[^a-z0-9+#]+", " ").trim();
    }

    // 2. separa em palavras
    public static List<String> tokenizar(String textoNormalizado) {
        List<String> palavras = new ArrayList<>();
        for (String palavra : textoNormalizado.split("\\s+")) {
            palavra = palavra.replaceAll("^[+#]+", "");
            if (!palavra.isEmpty()) {
                palavras.add(palavra);
            }
        }
        return palavras;
    }

    // 3. palavras que aparecem em qualquer frase e não ajudam a busca (resources/rag/stopwords-pt.txt)
    public static boolean isStopWord(String palavra) {
        return STOP_WORDS.contains(palavra);
    }

    // 1 a 4 juntos: palavras-chave sem repetir, na ordem da frase
    public static List<String> extrairPalavrasChave(String texto) {
        Set<String> resultado = new LinkedHashSet<>();
        for (String palavra : tokenizar(normalizar(texto))) {
            if (palavra.length() < 2 || isStopWord(palavra) || palavra.matches("\\d+")) {
                continue;
            }
            String singular = singular(palavra);
            if (!isStopWord(singular)) {
                resultado.add(singular);
            }
        }
        return new ArrayList<>(resultado);
    }

    // as palavras que mais aparecem no texto (usado para indexar os documentos)
    public static List<String> palavrasMaisFrequentes(String texto, int limite) {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (String palavra : tokenizar(normalizar(texto))) {
            if (palavra.length() >= 2 && !isStopWord(palavra) && !palavra.matches("\\d+")) {
                contagem.merge(singular(palavra), 1, Integer::sum);
            }
        }
        return contagem.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limite)
                .map(Map.Entry::getKey)
                .toList();
    }

    // plural simples: "conversas" -> "conversa", "padrões" -> "padrao", "valores" -> "valor"
    static String singular(String palavra) {
        if (palavra.length() <= 3 || palavra.endsWith("ss") || palavra.endsWith("us") || palavra.endsWith("is")) {
            return palavra;
        }
        if (palavra.endsWith("oes") || palavra.endsWith("aes")) {
            return palavra.substring(0, palavra.length() - 3) + "ao";
        }
        if (palavra.endsWith("res") || palavra.endsWith("zes")) {
            return palavra.substring(0, palavra.length() - 2);
        }
        return palavra.endsWith("s") ? palavra.substring(0, palavra.length() - 1) : palavra;
    }

    private static Set<String> carregarStopWords() {
        Set<String> palavras = new HashSet<>();
        InputStream arquivo = ProcessadorTexto.class.getResourceAsStream("/rag/stopwords-pt.txt");
        if (arquivo == null) {
            throw new IllegalStateException("Arquivo de stop words não encontrado");
        }
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(arquivo, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (!linha.startsWith("#") && !normalizar(linha).isEmpty()) {
                    palavras.add(normalizar(linha));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler as stop words", e);
        }
        return palavras;
    }
}
