package br.com.projeto;

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
import br.com.projeto.service.AuthenticationService;
import br.com.projeto.service.ChatService;
import br.com.projeto.service.ConversationService;
import br.com.projeto.service.GroqService;
import br.com.projeto.service.PasswordRecoveryService;
import br.com.projeto.service.RAGService;
import br.com.projeto.util.AppException;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Aplicação completa para testes: repositórios em memória (com as mesmas regras de filtro
 * das versões MongoDB) e uma IA falsa. Não precisa de banco nem de internet.
 */
public class TestSupport {

    private static final MongoCollection<Document> NO_DB = null;

    public final Users users = new Users();
    public final Conversations conversationRepo = new Conversations();
    public final Messages messageRepo = new Messages();
    public final Documents documentRepo = new Documents();
    public final Tokens tokens = new Tokens();
    public final FakeGroq groq = new FakeGroq();
    public final MutableClock clock = new MutableClock(Instant.parse("2026-10-10T12:00:00Z"));

    public final AuthenticationService auth = new AuthenticationService(users);
    public final ConversationService conversations = new ConversationService(conversationRepo, messageRepo, auth);
    public final RAGService rag = new RAGService(documentRepo, auth);
    public final ChatService chat = new ChatService(conversations, messageRepo, rag, groq, auth);
    public final PasswordRecoveryService recovery = new PasswordRecoveryService(auth, tokens, clock);

    private static String newId() {
        return new ObjectId().toHexString();
    }

    public static class Users extends UserRepository {
        public final Map<String, User> data = new HashMap<>();
        Users() { super(NO_DB); }
        @Override public User save(User u) { u.setId(newId()); data.put(u.getId(), u); return u; }
        @Override public Optional<User> findByEmail(String email) {
            return data.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }
        @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
        @Override public void updatePassword(String id, String hash) { data.get(id).setPasswordHash(hash); }
    }

    public static class Conversations extends ConversationRepository {
        public final Map<String, Conversation> data = new HashMap<>();
        Conversations() { super(NO_DB); }
        @Override public Conversation save(Conversation c) { c.setId(newId()); data.put(c.getId(), c); return c; }
        @Override public List<Conversation> findByUserId(String userId) {
            return data.values().stream().filter(c -> c.getUserId().equals(userId))
                    .sorted(Comparator.comparing(Conversation::getUpdatedAt).reversed()).toList();
        }
        @Override public Optional<Conversation> findByIdAndUserId(String id, String userId) {
            return Optional.ofNullable(data.get(id)).filter(c -> c.getUserId().equals(userId));
        }
        @Override public boolean update(String id, String userId, String title, Instant updatedAt) {
            return findByIdAndUserId(id, userId).map(c -> {
                if (title != null) c.setTitle(title);
                c.setUpdatedAt(updatedAt);
                return true;
            }).orElse(false);
        }
        @Override public boolean deleteByIdAndUserId(String id, String userId) {
            return findByIdAndUserId(id, userId).map(c -> data.remove(id) != null).orElse(false);
        }
    }

    public static class Messages extends MessageRepository {
        public final List<Message> data = new ArrayList<>();
        Messages() { super(NO_DB); }
        @Override public Message save(Message m) { m.setId(newId()); data.add(m); return m; }
        @Override public List<Message> findByConversation(String conversationId, String userId) {
            return data.stream().filter(m -> m.getConversationId().equals(conversationId) && m.getUserId().equals(userId))
                    .sorted(Comparator.comparing(Message::getCreatedAt)).toList();
        }
        @Override public long deleteByConversation(String conversationId, String userId) {
            List<Message> removed = findByConversation(conversationId, userId);
            data.removeAll(removed);
            return removed.size();
        }
    }

    public static class Documents extends DocumentRepository {
        public final Map<String, KnowledgeDocument> data = new HashMap<>();
        Documents() { super(NO_DB); }
        @Override public KnowledgeDocument save(KnowledgeDocument d) { d.setId(newId()); data.put(d.getId(), d); return d; }
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
        @Override public long countPublic() { return data.values().stream().filter(KnowledgeDocument::isPublic).count(); }
    }

    public static class Tokens extends PasswordResetRepository {
        public final Map<String, PasswordResetToken> data = new HashMap<>();
        Tokens() { super(NO_DB); }
        @Override public PasswordResetToken save(PasswordResetToken t) { t.setId(newId()); data.put(t.getId(), t); return t; }
        @Override public Optional<PasswordResetToken> findLatestUnused(String userId) {
            return data.values().stream().filter(t -> t.getUserId().equals(userId) && !t.isUsed())
                    .max(Comparator.comparing(PasswordResetToken::getCreatedAt));
        }
        @Override public void update(PasswordResetToken token) { data.put(token.getId(), token); }
        @Override public void invalidateAll(String userId) {
            data.values().stream().filter(t -> t.getUserId().equals(userId)).forEach(t -> t.setUsed(true));
        }
    }

    /** IA falsa: devolve uma resposta fixa e guarda os prompts recebidos. */
    public static class FakeGroq extends GroqService {
        public final List<List<Turn>> calls = new ArrayList<>();
        public boolean fail;
        public String answer = "Resposta de teste.";

        @Override public boolean isConfigured() { return true; }
        @Override public String complete(List<Turn> turns) {
            calls.add(turns);
            if (fail) throw new AppException("A API da Groq está indisponível no momento.");
            return answer;
        }
        public List<Turn> lastPrompt() { return calls.get(calls.size() - 1); }
    }

    public static class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        public void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
