package br.com.projeto.exception;

/** Falha na integração com a API da Groq. */
public class AiServiceException extends AppException {

    public AiServiceException(String userMessage) {
        super(userMessage);
    }

    public AiServiceException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
