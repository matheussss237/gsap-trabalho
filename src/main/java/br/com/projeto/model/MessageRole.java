package br.com.projeto.model;

/** Autor da mensagem. O valor de apiName é o esperado pela API da Groq. */
public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String apiName;

    MessageRole(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }
}
