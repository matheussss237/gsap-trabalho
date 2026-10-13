package br.com.projeto.exception;

/** Dados inválidos informados pelo usuário. */
public class ValidationException extends AppException {

    public ValidationException(String userMessage) {
        super(userMessage);
    }

    public ValidationException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
