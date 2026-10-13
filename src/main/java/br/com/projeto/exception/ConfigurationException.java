package br.com.projeto.exception;

/** Configuração externa ausente ou inválida (variáveis de ambiente). */
public class ConfigurationException extends AppException {

    public ConfigurationException(String userMessage) {
        super(userMessage);
    }

    public ConfigurationException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
