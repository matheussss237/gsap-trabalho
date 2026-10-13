package br.com.projeto.support;

import br.com.projeto.model.Conversation;
import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.model.Message;
import br.com.projeto.model.PasswordResetToken;
import br.com.projeto.model.User;
import br.com.projeto.repository.ConversationRepository;
import br.com.projeto.repository.DocumentRepository;
import br.com.projeto.repository.MessageRepository;
import br.com.projeto.repository.PasswordResetRepository;
import br.com.projeto.repository.UserRepository;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementações em memória dos repositórios, com as mesmas regras de filtro das versões MongoDB. */
public final class InMemoryRepositories {

    private InMemoryRepositories() {
    }

    private static String newId() {
        return new ObjectId().toHexString();
    }

    public static class Users implements UserRepository {
        public final Map<String, User> data = new ConcurrentHashMap<>();

        @Override public User save(User user) {
            user.setId(newId());
            data.put(user.getId(), user);
            return user;
        }
        @Override public Optional<User> findById(String id) { return Optional.ofNullable(data.get(id)); }
        @Override public Optional<User> findByEmail(String email) {
            return data.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }
        @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
        @Override public void updatePassword(String userId, String hash, Instant updatedAt) {
            findById(userId).ifPresent(u -> { u.setPasswordHash(hash); u.setUpdatedAt(updatedAt); });
        }
    }

    public static class Conversations implements ConversationRepository {
        public final Map<String, Conversation> data = new ConcurrentHashMap<>();

        @Override public Conversation save(Conversation c) {
            c.setId(newId());
            data.put(c.getId(), c);
            return c;
        }
        @Override public List<Conversation> findByUserId(String userId) {
            return data.values().stream().filter(c -> c.getUserId().equals(userId))
                    .sorted(Comparator.comparing(Conversation::getUpdatedAt).reversed()).toList();
        }
        @Override public Optional<Conversation> findByIdAndUserId(String id, String userId) {
            return Optional.ofNullable(data.get(id)).filter(c -> c.getUserId().equals(userId));
        }
        @Override public boolean updateTitle(String id, String userId, String title, Instant updatedAt) {
            return findByIdAndUserId(id, userId).map(c -> { c.setTitle(title); c.setUpdatedAt(updatedAt); return true; })
                    .orElse(false);
        }
        @Override public boolean touch(String id, String userId, Instant updatedAt) {
            return findByIdAndUserId(id, userId).map(c -> { c.setUpdatedAt(updatedAt); return true; }).orElse(false);
        }
        @Override public boolean deleteByIdAndUserId(String id, String userId) {
            return findByIdAndUserId(id, userId).map(c -> data.remove(id) != null).orElse(false);
        }
    }

    public static class Messages implements MessageRepository {
        public final List<Message> data = new ArrayList<>();

        @Override public synchronized Message save(Message m) {
            m.setId(newId());
            data.add(m);
            return m;
        }
        @Override public synchronized List<Message> findByConversation(String conversationId, String userId) {
            return data.stream().filter(m -> m.getConversationId().equals(conversationId) && m.getUserId().equals(userId))
                    .sorted(Comparator.comparing(Message::getCreatedAt)).toList();
        }
        @Override public synchronized long deleteByConversation(String conversationId, String userId) {
            List<Message> toRemove = findByConversation(conversationId, userId);
            data.removeAll(toRemove);
            return toRemove.size();
        }
    }

    public static class Documents implements DocumentRepository {
        public final Map<String, KnowledgeDocument> data = new ConcurrentHashMap<>();

        @Override public KnowledgeDocument save(KnowledgeDocument d) {
            d.setId(newId());
            data.put(d.getId(), d);
            return d;
        }
        @Override public List<KnowledgeDocument> findVisibleTo(String userId) {
            return data.values().stream().filter(d -> d.getOwnerId() == null || d.getOwnerId().equals(userId))
                    .sorted(Comparator.comparing(KnowledgeDocument::getTitle)).toList();
        }
        @Override public List<KnowledgeDocument> findByAnyKeyword(Collection<String> keywords, String userId) {
            return findVisibleTo(userId).stream().filter(d -> d.getKeywords().stream().anyMatch(keywords::contains)).toList();
        }
        @Override public boolean deleteByIdAndOwner(String id, String ownerId) {
            KnowledgeDocument d = data.get(id);
            return d != null && Objects.equals(d.getOwnerId(), ownerId) && data.remove(id) != null;
        }
        @Override public long countPublic() {
            return data.values().stream().filter(KnowledgeDocument::isPublic).count();
        }
    }

    public static class ResetTokens implements PasswordResetRepository {
        public final Map<String, PasswordResetToken> data = new ConcurrentHashMap<>();

        @Override public PasswordResetToken save(PasswordResetToken t) {
            t.setId(newId());
            data.put(t.getId(), t);
            return t;
        }
        @Override public Optional<PasswordResetToken> findLatestUnused(String userId) {
            return data.values().stream().filter(t -> t.getUserId().equals(userId) && !t.isUsed())
                    .max(Comparator.comparing(PasswordResetToken::getCreatedAt));
        }
        @Override public void update(PasswordResetToken token) { data.put(token.getId(), token); }
        @Override public void invalidateAll(String userId) {
            data.values().stream().filter(t -> t.getUserId().equals(userId)).forEach(t -> t.setUsed(true));
        }
    }
}
