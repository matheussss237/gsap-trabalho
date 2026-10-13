package br.com.projeto.util;

import br.com.projeto.exception.ValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Validações de entrada reutilizadas pelos Services. */
public final class Validator {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_NAME_LENGTH = 60;
    public static final int MAX_TITLE_LENGTH = 80;

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    private Validator() {
    }

    public static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("O campo \"" + fieldName + "\" é obrigatório.");
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
            throw new ValidationException("Informe um e-mail válido, por exemplo: nome@empresa.com.");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public static String name(String name) {
        String value = required(name, "Nome");
        if (value.length() < 2) {
            throw new ValidationException("O nome deve ter pelo menos 2 caracteres.");
        }
        if (value.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("O nome deve ter no máximo " + MAX_NAME_LENGTH + " caracteres.");
        }
        return value;
    }

    /** Senha: tamanho mínimo, pelo menos uma letra e um número, e confirmação igual. */
    public static void newPassword(String password, String confirmation) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("O campo \"Senha\" é obrigatório.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("A senha deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        if (password.length() > 72) {
            throw new ValidationException("A senha deve ter no máximo 72 caracteres.");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ValidationException("A senha deve conter letras e números.");
        }
        if (!password.equals(confirmation)) {
            throw new ValidationException("A confirmação da senha não confere.");
        }
    }

    public static String title(String title) {
        String value = required(title, "Título");
        if (value.length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("O título deve ter no máximo " + MAX_TITLE_LENGTH + " caracteres.");
        }
        return value;
    }
}
