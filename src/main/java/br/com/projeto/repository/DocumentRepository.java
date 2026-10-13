package br.com.projeto.repository;

import br.com.projeto.model.KnowledgeDocument;

import java.util.Collection;
import java.util.List;

/** Acesso aos dados da coleção "documents" (base de conhecimento do RAG). */
public interface DocumentRepository {

    KnowledgeDocument save(KnowledgeDocument document);

    /** Documentos públicos + documentos do próprio usuário. */
    List<KnowledgeDocument> findVisibleTo(String userId);

    /** Documentos visíveis ao usuário que possuam ao menos uma das palavras-chave. */
    List<KnowledgeDocument> findByAnyKeyword(Collection<String> keywords, String userId);

    boolean deleteByIdAndOwner(String id, String ownerId);

    long countPublic();
}
