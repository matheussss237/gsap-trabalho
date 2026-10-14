package br.com.projeto.model;

import java.time.Instant;

/** Usuário do sistema. A senha nunca é guardada em texto puro, apenas o hash BCrypt. */
public class User {

    private String id;
    private String name;
    private String email;
    private String passwordHash;
    private Instant createdAt;
    private Instant updatedAt;

    public User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    /** Primeiro nome, usado nas saudações e para personalizar a IA. */
    public String firstName() {
        return name == null || name.isBlank() ? "" : name.trim().split("\\s+")[0];
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
