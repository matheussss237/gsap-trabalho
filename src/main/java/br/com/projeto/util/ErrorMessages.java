package br.com.projeto.util;

import br.com.projeto.exception.AppException;

import java.lang.System.Logger.Level;

/** Traduz exceções em mensagens compreensíveis; o detalhe técnico vai apenas para o log. */
public final class ErrorMessages {

    private static final System.Logger LOG = System.getLogger("NexusChat");

    private ErrorMessages() {
    }

    public static String friendly(Throwable error) {
        Throwable root = error;
        while (!(root instanceof AppException) && root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root instanceof AppException app) {
            if (app.getCause() != null) {
                LOG.log(Level.WARNING, app.getMessage(), app.getCause());
            }
            return app.getMessage();
        }
        LOG.log(Level.ERROR, "Erro inesperado", error);
        return "Ocorreu um erro inesperado. Tente novamente em instantes.";
    }
}
