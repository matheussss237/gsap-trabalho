package br.com.projeto.repository;

import br.com.projeto.model.PasswordResetToken;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Optional;

import static br.com.projeto.config.AppConfig.*;

/** Acesso à coleção "password_reset_tokens". */
public class PasswordResetRepository {

    private final MongoCollection<Document> collection;

    public PasswordResetRepository(MongoDatabase database) {
        this(database.getCollection("password_reset_tokens"));
    }

    protected PasswordResetRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public PasswordResetToken save(PasswordResetToken token) {
        return db("gerar o código de recuperação", () -> {
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

    /** Código mais recente ainda não utilizado do usuário. */
    public Optional<PasswordResetToken> findLatestUnused(String userId) {
        return db("validar o código de recuperação", () -> Optional.ofNullable(collection
                .find(Filters.and(Filters.eq("userId", userId), Filters.eq("used", false)))
                .sort(Sorts.descending("createdAt")).first()).map(this::toToken));
    }

    public void update(PasswordResetToken token) {
        objectId(token.getId()).ifPresent(oid -> db("atualizar o código de recuperação", () -> collection.updateOne(
                Filters.eq("_id", oid),
                Updates.combine(Updates.set("used", token.isUsed()), Updates.set("attempts", token.getAttempts())))));
    }

    public void invalidateAll(String userId) {
        db("invalidar códigos anteriores", () -> collection.updateMany(
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
