package br.com.projeto.repository.mongo;

import br.com.projeto.model.PasswordResetToken;
import br.com.projeto.repository.PasswordResetRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Optional;

import static br.com.projeto.repository.mongo.MongoSupport.*;

public class MongoPasswordResetRepository implements PasswordResetRepository {

    private final MongoCollection<Document> collection;

    public MongoPasswordResetRepository(MongoDatabase database) {
        this.collection = database.getCollection("password_reset_tokens");
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return execute("gerar o código de recuperação", () -> {
            ObjectId id = new ObjectId();
            collection.insertOne(new Document("_id", id)
                    .append("userId", token.getUserId())
                    .append("tokenHash", token.getTokenHash())
                    .append("createdAt", toDate(token.getCreatedAt()))
                    .append("expiresAt", toDate(token.getExpiresAt()))
                    .append("used", token.isUsed())
                    .append("attempts", token.getAttempts()));
            token.setId(id.toHexString());
            return token;
        });
    }

    @Override
    public Optional<PasswordResetToken> findLatestUnused(String userId) {
        return execute("validar o código de recuperação", () -> Optional.ofNullable(collection
                .find(Filters.and(Filters.eq("userId", userId), Filters.eq("used", false)))
                .sort(Sorts.descending("createdAt"))
                .first()).map(this::toToken));
    }

    @Override
    public void update(PasswordResetToken token) {
        objectId(token.getId()).ifPresent(oid -> run("atualizar o código de recuperação", () -> collection.updateOne(
                Filters.eq("_id", oid),
                Updates.combine(Updates.set("used", token.isUsed()), Updates.set("attempts", token.getAttempts())))));
    }

    @Override
    public void invalidateAll(String userId) {
        run("invalidar códigos anteriores", () -> collection.updateMany(
                Filters.and(Filters.eq("userId", userId), Filters.eq("used", false)), Updates.set("used", true)));
    }

    private PasswordResetToken toToken(Document doc) {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(doc.getObjectId("_id").toHexString());
        t.setUserId(doc.getString("userId"));
        t.setTokenHash(doc.getString("tokenHash"));
        t.setCreatedAt(toInstant(doc.getDate("createdAt")));
        t.setExpiresAt(toInstant(doc.getDate("expiresAt")));
        t.setUsed(doc.getBoolean("used", false));
        t.setAttempts(doc.getInteger("attempts", 0));
        return t;
    }
}
