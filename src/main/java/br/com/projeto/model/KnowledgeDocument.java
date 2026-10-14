package br.com.projeto.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento da base de conhecimento do RAG (coleção "documents").
 * ownerId nulo = documento público (exemplo); caso contrário pertence a um usuário.
 */
public class KnowledgeDocument {

    private String id;
    private String ownerId;
    private String title;
    private String content;
    private List<String> keywords = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public KnowledgeDocument() {
    }

    public KnowledgeDocument(String ownerId, String title, String content, List<String> keywords) {
        this.ownerId = ownerId;
        this.title = title;
        this.content = content;
        setKeywords(keywords);
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public boolean isPublic() {
        return ownerId == null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords == null ? new ArrayList<>() : new ArrayList<>(keywords); }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
