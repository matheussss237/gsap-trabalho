package br.com.projeto.repository;

import br.com.projeto.model.Conversation;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static br.com.projeto.config.AppConfig.*;

/**
 * Acesso à coleção "conversations".
 * Toda consulta, alteração e exclusão filtra por _id E userId: o isolamento entre usuários
 * é garantido pelo banco, não apenas pela interface.
 */
public class ConversationRepository {

    private final MongoCollection<Document> collection;

    public ConversationRepository(MongoDatabase database) {
        this(database.getCollection("conversations"));
    }

    protected ConversationRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public Conversation save(Conversation conversation) {
        return db("criar a conversa", () -> {
            ObjectId id = new ObjectId();
            collection.insertOne(new Document("_id", id)
                    .append("userId", conversation.getUserId())
                    .append("title", conversation.getTitle())
                    .append("createdAt", toDate(conversation.getCreatedAt()))
                    .append("updatedAt", toDate(conversation.getUpdatedAt())));
            conversation.setId(id.toHexString());
            return conversation;
        });
    }

    public List<Conversation> findByUserId(String userId) {
        return db("carregar as conversas", () -> collection.find(Filters.eq("userId", userId))
                .sort(Sorts.descending("updatedAt"))
                .map(this::toConversation)
                .into(new ArrayList<>()));
    }

    public Optional<Conversation> findByIdAndUserId(String id, String userId) {
        return ownedBy(id, userId).flatMap(filter -> db("carregar a conversa",
                () -> Optional.ofNullable(collection.find(filter).first()).map(this::toConversation)));
    }

    /** Altera título e/ou data de atualização. Retorna false se a conversa não for do usuário. */
    public boolean update(String id, String userId, String newTitle, Instant updatedAt) {
        Bson changes = newTitle == null
                ? Updates.set("updatedAt", toDate(updatedAt))
                : Updates.combine(Updates.set("title", newTitle), Updates.set("updatedAt", toDate(updatedAt)));
        return ownedBy(id, userId).map(filter -> db("atualizar a conversa",
                () -> collection.updateOne(filter, changes).getMatchedCount() > 0)).orElse(false);
    }

    public boolean deleteByIdAndUserId(String id, String userId) {
        return ownedBy(id, userId).map(filter -> db("excluir a conversa",
                () -> collection.deleteOne(filter).getDeletedCount() > 0)).orElse(false);
    }

    private Optional<Bson> ownedBy(String id, String userId) {
        return objectId(id).map(oid -> Filters.and(Filters.eq("_id", oid), Filters.eq("userId", userId)));
    }

    private Conversation toConversation(Document doc) {
        Conversation c = new Conversation();
        c.setId(doc.getObjectId("_id").toHexString());
        c.setUserId(doc.getString("userId"));
        c.setTitle(doc.getString("title"));
        c.setCreatedAt(toInstant(doc.getDate("createdAt")));
        c.setUpdatedAt(toInstant(doc.getDate("updatedAt")));
        return c;
    }
}
