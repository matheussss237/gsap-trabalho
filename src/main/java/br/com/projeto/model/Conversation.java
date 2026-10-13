package br.com.projeto.model;

import java.time.Instant;

/** Conversa pertencente a um único usuário (userId = proprietário). */
public class Conversation {

    public static final String DEFAULT_TITLE = "Nova conversa";

    private String id;
    private String userId;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;

    public Conversation() {
    }

    public Conversation(String userId, String title) {
        this.userId = userId;
        this.title = title;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public boolean hasDefaultTitle() {
        return DEFAULT_TITLE.equals(title);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return title;
    }
}
