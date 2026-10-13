package br.com.projeto.model;

/** Resultado de um envio: a mensagem do usuário e a resposta da IA, já persistidas. */
public record ChatReply(Message userMessage, Message assistantMessage, Conversation conversation) {
}
