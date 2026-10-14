package br.com.projeto.util;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/** Validação de entradas, hash de senhas (BCrypt) e códigos temporários. */
public final class Security {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_TITLE_LENGTH = 80;

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private Security() {
    }

    // ---------- validação ----------

    public static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new AppException("O campo \"" + fieldName + "\" é obrigatório.");
        }
        return value.trim();
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= 254 && EMAIL.matcher(email.trim()).matches();
    }

    /** Valida e normaliza o e-mail (minúsculas, sem espaços). */
    public static String email(String email) {
        String value = required(email, "E-mail");
        if (!isValidEmail(value)) {
            throw new AppException("Informe um e-mail válido, por exemplo: nome@empresa.com.");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public static String name(String name) {
        String value = required(name, "Nome");
        if (value.length() < 2 || value.length() > 60) {
            throw new AppException("O nome deve ter entre 2 e 60 caracteres.");
        }
        return value;
    }

    /** Senha: tamanho mínimo, letras e números, e confirmação igual. */
    public static void newPassword(String password, String confirmation) {
        if (password == null || password.isEmpty()) {
            throw new AppException("O campo \"Senha\" é obrigatório.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > 72) {
            throw new AppException("A senha deve ter entre " + MIN_PASSWORD_LENGTH + " e 72 caracteres.");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new AppException("A senha deve conter letras e números.");
        }
        if (!password.equals(confirmation)) {
            throw new AppException("A confirmação da senha não confere.");
        }
    }

    public static String title(String title) {
        String value = required(title, "Título");
        if (value.length() > MAX_TITLE_LENGTH) {
            throw new AppException("O título deve ter no máximo " + MAX_TITLE_LENGTH + " caracteres.");
        }
        return value;
    }

    // ---------- senhas ----------

    /** BCrypt: salt aleatório embutido e custo 12. O hash não pode ser "descriptografado". */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean checkPassword(String plainPassword, String hash) {
        if (plainPassword == null || hash == null || !hash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ---------- códigos temporários ----------

    /** Código numérico de 6 dígitos gerado com SecureRandom. */
    public static String newCode() {
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

    /** Comparação em tempo constante (evita ataques de temporização). */
    public static boolean sameHash(String a, String b) {
        return a != null && b != null
                && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
