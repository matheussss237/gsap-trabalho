package br.com.projeto;

/**
 * Classe principal do JAR/instalador. Não estende Application para que o JavaFX
 * possa ser carregado a partir do classpath (pasta libs/) sem module-path.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
