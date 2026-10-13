package br.com.projeto.repository.mongo;

import br.com.projeto.model.Conversation;
import br.com.projeto.repository.ConversationRepository;
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

import static br.com.projeto.repository.mongo.MongoSupport.*;

public class MongoConversationRepository implements ConversationRepository {

    private final MongoCollection<Document> collection;

    public MongoConversationRepository(MongoDatabase database) {
        this.collection = database.getCollection("conversations");
    }

    @Override
    public Conversation save(Conversation conversation) {
        return execute("criar a conversa", () -> {
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

    @Override
    public List<Conversation> findByUserId(String userId) {
        return execute("carregar as conversas", () -> collection.find(Filters.eq("userId", userId))
                .sort(Sorts.descending("updatedAt"))
                .map(this::toConversation)
                .into(new ArrayList<>()));
    }

    @Override
    public Optional<Conversation> findByIdAndUserId(String id, String userId) {
        return ownedBy(id, userId).flatMap(filter -> execute("carregar a conversa",
                () -> Optional.ofNullable(collection.find(filter).first()).map(this::toConversation)));
    }

    @Override
    public boolean updateTitle(String id, String userId, String title, Instant updatedAt) {
        return ownedBy(id, userId).map(filter -> execute("renomear a conversa", () -> collection.updateOne(filter,
                Updates.combine(Updates.set("title", title), Updates.set("updatedAt", toDate(updatedAt))))
                .getMatchedCount() > 0)).orElse(false);
    }

    @Override
    public boolean touch(String id, String userId, Instant updatedAt) {
        return ownedBy(id, userId).map(filter -> execute("atualizar a conversa", () -> collection.updateOne(filter,
                Updates.set("updatedAt", toDate(updatedAt))).getMatchedCount() > 0)).orElse(false);
    }

    @Override
    public boolean deleteByIdAndUserId(String id, String userId) {
        return ownedBy(id, userId).map(filter -> execute("excluir a conversa",
                () -> collection.deleteOne(filter).getDeletedCount() > 0)).orElse(false);
    }

    /** Filtro que só encontra a conversa se ela pertencer ao usuário informado. */
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
