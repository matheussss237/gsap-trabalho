package br.com.projeto.exception;

/** Registro inexistente ou que não pertence ao usuário autenticado. */
public class NotFoundException extends AppException {

    public NotFoundException(String userMessage) {
        super(userMessage);
    }

    public NotFoundException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
