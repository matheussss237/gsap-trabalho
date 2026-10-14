package br.com.projeto.repository;

import br.com.projeto.model.KnowledgeDocument;
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

import static br.com.projeto.config.AppConfig.*;

/** Acesso à coleção "documents" (base de conhecimento do RAG). */
public class DocumentRepository {

    private final MongoCollection<Document> collection;

    public DocumentRepository(MongoDatabase database) {
        this(database.getCollection("documents"));
    }

    protected DocumentRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public KnowledgeDocument save(KnowledgeDocument document) {
        return db("salvar o documento", () -> {
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

    /** Documentos públicos + documentos do próprio usuário. */
    public List<KnowledgeDocument> findVisibleTo(String userId) {
        return db("carregar a base de conhecimento", () -> collection.find(visibleTo(userId))
                .sort(Sorts.ascending("title")).map(this::toDocument).into(new ArrayList<>()));
    }

    /** Documentos visíveis ao usuário que tenham ao menos uma das palavras-chave. */
    public List<KnowledgeDocument> findByAnyKeyword(Collection<String> keywords, String userId) {
        if (keywords.isEmpty()) {
            return List.of();
        }
        return db("pesquisar a base de conhecimento", () -> collection
                .find(Filters.and(visibleTo(userId), Filters.in("keywords", keywords)))
                .map(this::toDocument).into(new ArrayList<>()));
    }

    public boolean deleteByIdAndOwner(String id, String ownerId) {
        return objectId(id).map(oid -> db("excluir o documento", () -> collection
                .deleteOne(Filters.and(Filters.eq("_id", oid), Filters.eq("ownerId", ownerId)))
                .getDeletedCount() > 0)).orElse(false);
    }

    public long countPublic() {
        return db("verificar a base de conhecimento", () -> collection.countDocuments(Filters.eq("ownerId", null)));
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
