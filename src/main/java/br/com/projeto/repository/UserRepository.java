package br.com.projeto.repository;

import br.com.projeto.model.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Optional;

import static br.com.projeto.config.AppConfig.*;

/** Acesso à coleção "users". */
public class UserRepository {

    private final MongoCollection<Document> collection;

    public UserRepository(MongoDatabase database) {
        this(database.getCollection("users"));
    }

    protected UserRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public User save(User user) {
        return db("salvar o usuário", () -> {
            ObjectId id = new ObjectId();
            collection.insertOne(new Document("_id", id)
                    .append("name", user.getName())
                    .append("email", user.getEmail())
                    .append("passwordHash", user.getPasswordHash())
                    .append("createdAt", toDate(user.getCreatedAt()))
                    .append("updatedAt", toDate(user.getUpdatedAt())));
            user.setId(id.toHexString());
            return user;
        });
    }

    public Optional<User> findByEmail(String email) {
        return db("buscar o usuário",
                () -> Optional.ofNullable(collection.find(Filters.eq("email", email)).first()).map(this::toUser));
    }

    public boolean existsByEmail(String email) {
        return db("verificar o e-mail", () -> collection.countDocuments(Filters.eq("email", email)) > 0);
    }

    public void updatePassword(String userId, String passwordHash) {
        objectId(userId).ifPresent(oid -> db("atualizar a senha", () -> collection.updateOne(Filters.eq("_id", oid),
                Updates.combine(Updates.set("passwordHash", passwordHash),
                        Updates.set("updatedAt", toDate(Instant.now()))))));
    }

    private User toUser(Document doc) {
        User user = new User();
        user.setId(doc.getObjectId("_id").toHexString());
        user.setName(doc.getString("name"));
        user.setEmail(doc.getString("email"));
        user.setPasswordHash(doc.getString("passwordHash"));
        user.setCreatedAt(toInstant(doc.getDate("createdAt")));
        user.setUpdatedAt(toInstant(doc.getDate("updatedAt")));
        return user;
    }
}
