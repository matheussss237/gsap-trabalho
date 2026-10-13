package br.com.projeto.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Hash de senhas com BCrypt (salt aleatório embutido + custo configurável).
 * Não é possível "descriptografar" o hash: a verificação refaz o cálculo e compara.
 */
public final class PasswordHasher {

    private static final int COST = 12;

    private PasswordHasher() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST));
    }

    public static boolean matches(String plainPassword, String hash) {
        if (plainPassword == null || hash == null || !hash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
