package br.com.projeto.repository.mongo;

import br.com.projeto.model.User;
import br.com.projeto.repository.UserRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Optional;

import static br.com.projeto.repository.mongo.MongoSupport.*;

public class MongoUserRepository implements UserRepository {

    private final MongoCollection<Document> collection;

    public MongoUserRepository(MongoDatabase database) {
        this.collection = database.getCollection("users");
    }

    @Override
    public User save(User user) {
        return execute("salvar o usuário", () -> {
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

    @Override
    public Optional<User> findById(String id) {
        return objectId(id).flatMap(oid -> execute("buscar o usuário",
                () -> Optional.ofNullable(collection.find(Filters.eq("_id", oid)).first()).map(this::toUser)));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return execute("buscar o usuário",
                () -> Optional.ofNullable(collection.find(Filters.eq("email", email)).first()).map(this::toUser));
    }

    @Override
    public boolean existsByEmail(String email) {
        return execute("verificar o e-mail", () -> collection.countDocuments(Filters.eq("email", email)) > 0);
    }

    @Override
    public void updatePassword(String userId, String passwordHash, Instant updatedAt) {
        objectId(userId).ifPresent(oid -> run("atualizar a senha", () -> collection.updateOne(
                Filters.eq("_id", oid),
                Updates.combine(Updates.set("passwordHash", passwordHash),
                        Updates.set("updatedAt", toDate(updatedAt))))));
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
