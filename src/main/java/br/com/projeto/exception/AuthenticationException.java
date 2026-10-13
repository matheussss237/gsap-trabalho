package br.com.projeto.exception;

/** Falha de autenticação (usuário inexistente, senha incorreta, sessão ausente). */
public class AuthenticationException extends AppException {

    public AuthenticationException(String userMessage) {
        super(userMessage);
    }

    public AuthenticationException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
