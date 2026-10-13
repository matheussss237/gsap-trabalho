package br.com.projeto.exception;

/**
 * Exceção base da aplicação. A mensagem é sempre escrita para o usuário final
 * (compreensível, sem detalhes técnicos); a causa técnica fica em getCause().
 */
public class AppException extends RuntimeException {

    public AppException(String userMessage) {
        super(userMessage);
    }

    public AppException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
