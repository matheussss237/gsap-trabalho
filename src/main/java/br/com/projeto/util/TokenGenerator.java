package br.com.projeto.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Geração e hash de códigos temporários (recuperação de senha). */
public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    /** Código numérico de 6 dígitos gerado com SecureRandom. */
    public static String numericCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /** Comparação em tempo constante, evitando ataques de temporização. */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
