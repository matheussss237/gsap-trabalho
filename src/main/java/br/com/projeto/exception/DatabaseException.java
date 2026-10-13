package br.com.projeto.exception;

/** Falha de comunicação com o MongoDB. */
public class DatabaseException extends AppException {

    public DatabaseException(String userMessage) {
        super(userMessage);
    }

    public DatabaseException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
