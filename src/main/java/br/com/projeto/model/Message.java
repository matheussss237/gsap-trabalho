package br.com.projeto.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mensagem de uma conversa. Guarda usuário, conversa, autor, conteúdo e data/hora.
 * Para respostas da IA, "sources" registra os títulos dos documentos usados pelo RAG.
 */
public class Message {

    private String id;
    private String conversationId;
    private String userId;
    private MessageRole role;
    private String content;
    private List<String> sources = new ArrayList<>();
    private Instant createdAt;

    public Message() {
    }

    public Message(String conversationId, String userId, MessageRole role, String content) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public MessageRole getRole() { return role; }
    public void setRole(MessageRole role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) {
        this.sources = sources == null ? new ArrayList<>() : new ArrayList<>(sources);
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
