package br.com.projeto.repository;

import br.com.projeto.model.Message;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static br.com.projeto.config.AppConfig.*;

/** Acesso à coleção "messages". Sempre filtrado por conversa E usuário. */
public class MessageRepository {

    private final MongoCollection<Document> collection;

    public MessageRepository(MongoDatabase database) {
        this(database.getCollection("messages"));
    }

    protected MessageRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public Message save(Message message) {
        return db("salvar a mensagem", () -> {
            ObjectId id = new ObjectId();
            collection.insertOne(new Document("_id", id)
                    .append("conversationId", message.getConversationId())
                    .append("userId", message.getUserId())
                    .append("role", message.getRole().name())
                    .append("content", message.getContent())
                    .append("sources", message.getSources())
                    .append("createdAt", toDate(message.getCreatedAt())));
            message.setId(id.toHexString());
            return message;
        });
    }

    public List<Message> findByConversation(String conversationId, String userId) {
        return db("carregar o histórico", () -> collection.find(filter(conversationId, userId))
                .sort(Sorts.ascending("createdAt", "_id"))
                .map(this::toMessage)
                .into(new ArrayList<>()));
    }

    public long deleteByConversation(String conversationId, String userId) {
        return db("excluir as mensagens", () -> collection.deleteMany(filter(conversationId, userId)).getDeletedCount());
    }

    private Bson filter(String conversationId, String userId) {
        return Filters.and(Filters.eq("conversationId", conversationId), Filters.eq("userId", userId));
    }

    private Message toMessage(Document doc) {
        Message m = new Message();
        m.setId(doc.getObjectId("_id").toHexString());
        m.setConversationId(doc.getString("conversationId"));
        m.setUserId(doc.getString("userId"));
        m.setRole(Message.Role.valueOf(doc.getString("role")));
        m.setContent(doc.getString("content"));
        m.setSources(doc.getList("sources", String.class));
        m.setCreatedAt(toInstant(doc.getDate("createdAt")));
        return m;
    }
}
