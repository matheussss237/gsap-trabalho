package br.com.projeto.repository.mongo;

import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.repository.DocumentRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static br.com.projeto.repository.mongo.MongoSupport.*;

public class MongoDocumentRepository implements DocumentRepository {

    private final MongoCollection<Document> collection;

    public MongoDocumentRepository(MongoDatabase database) {
        this.collection = database.getCollection("documents");
    }

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        return execute("salvar o documento", () -> {
            ObjectId id = new ObjectId();
            collection.insertOne(new Document("_id", id)
                    .append("ownerId", document.getOwnerId())
                    .append("title", document.getTitle())
                    .append("content", document.getContent())
                    .append("keywords", document.getKeywords())
                    .append("createdAt", toDate(document.getCreatedAt()))
                    .append("updatedAt", toDate(document.getUpdatedAt())));
            document.setId(id.toHexString());
            return document;
        });
    }

    @Override
    public List<KnowledgeDocument> findVisibleTo(String userId) {
        return execute("carregar a base de conhecimento", () -> collection.find(visibleTo(userId))
                .sort(Sorts.ascending("title"))
                .map(this::toDocument)
                .into(new ArrayList<>()));
    }

    @Override
    public List<KnowledgeDocument> findByAnyKeyword(Collection<String> keywords, String userId) {
        if (keywords.isEmpty()) {
            return List.of();
        }
        return execute("pesquisar a base de conhecimento", () -> collection
                .find(Filters.and(visibleTo(userId), Filters.in("keywords", keywords)))
                .map(this::toDocument)
                .into(new ArrayList<>()));
    }

    @Override
    public boolean deleteByIdAndOwner(String id, String ownerId) {
        return objectId(id).map(oid -> execute("excluir o documento", () -> collection
                .deleteOne(Filters.and(Filters.eq("_id", oid), Filters.eq("ownerId", ownerId)))
                .getDeletedCount() > 0)).orElse(false);
    }

    @Override
    public long countPublic() {
        return execute("verificar a base de conhecimento", () -> collection.countDocuments(Filters.eq("ownerId", null)));
    }

    private Bson visibleTo(String userId) {
        return Filters.or(Filters.eq("ownerId", null), Filters.eq("ownerId", userId));
    }

    private KnowledgeDocument toDocument(Document doc) {
        KnowledgeDocument d = new KnowledgeDocument();
        d.setId(doc.getObjectId("_id").toHexString());
        d.setOwnerId(doc.getString("ownerId"));
        d.setTitle(doc.getString("title"));
        d.setContent(doc.getString("content"));
        d.setKeywords(doc.getList("keywords", String.class));
        d.setCreatedAt(toInstant(doc.getDate("createdAt")));
        d.setUpdatedAt(toInstant(doc.getDate("updatedAt")));
        return d;
    }
}
