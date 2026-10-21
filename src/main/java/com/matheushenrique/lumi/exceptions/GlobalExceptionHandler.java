package com.matheushenrique.lumi.exceptions;

import com.mongodb.MongoException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;

import java.util.logging.Level;
import java.util.logging.Logger;

// transforma qualquer erro em uma mensagem que o usuário entende (sem stack trace na tela)
public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger("Lumi");

    private GlobalExceptionHandler() {
    }

    public static String mensagem(Throwable ex) {
        for (Throwable erro = ex; erro != null; erro = erro.getCause()) {
            if (erro instanceof RegraNegocioException || erro instanceof ResourceNotFoundException) {
                return erro.getMessage();
            }
            if (erro instanceof DuplicateKeyException) {
                return "Já existe um cadastro com esses dados.";
            }
            if (erro instanceof IllegalArgumentException && String.valueOf(erro.getMessage()).contains("connection string")) {
                return "A variável MONGODB_URI não foi configurada ou está inválida. Veja o README.";
            }
            if (erro instanceof MongoException || erro instanceof DataAccessException) {
                LOG.log(Level.WARNING, "Erro no MongoDB", ex);
                return "Não foi possível falar com o MongoDB Atlas. Verifique sua internet, a MONGODB_URI "
                        + "e se o seu IP está liberado em Network Access.";
            }
        }
        LOG.log(Level.SEVERE, "Erro inesperado", ex);
        return "Ocorreu um erro inesperado. Tente novamente.";
    }
}
