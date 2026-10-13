package br.com.projeto.repository.mongo;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.util.concurrent.TimeUnit;

/** Cria (de forma idempotente) os índices usados pela aplicação. */
public final class MongoIndexes {

    private MongoIndexes() {
    }

    public static void ensure(MongoDatabase db) {
        MongoSupport.run("preparar o banco de dados", () -> {
            db.getCollection("users").createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
            db.getCollection("conversations").createIndex(Indexes.compoundIndex(
                    Indexes.ascending("userId"), Indexes.descending("updatedAt")));
            db.getCollection("messages").createIndex(Indexes.compoundIndex(
                    Indexes.ascending("conversationId"), Indexes.ascending("userId"), Indexes.ascending("createdAt")));
            db.getCollection("documents").createIndex(Indexes.ascending("keywords"));
            db.getCollection("documents").createIndex(Indexes.ascending("ownerId"));
            db.getCollection("password_reset_tokens").createIndex(Indexes.ascending("userId"));
            // TTL: o próprio MongoDB remove tokens expirados
            db.getCollection("password_reset_tokens").createIndex(Indexes.ascending("expiresAt"),
                    new IndexOptions().expireAfter(0L, TimeUnit.SECONDS));
        });
    }
}
