package br.com.projeto.repository;

import br.com.projeto.model.Conversation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Acesso aos dados da coleção "conversations".
 * Todas as consultas, alterações e exclusões exigem o userId do proprietário:
 * o isolamento não depende da interface gráfica.
 */
public interface ConversationRepository {

    Conversation save(Conversation conversation);

    List<Conversation> findByUserId(String userId);

    Optional<Conversation> findByIdAndUserId(String id, String userId);

    boolean updateTitle(String id, String userId, String title, Instant updatedAt);

    boolean touch(String id, String userId, Instant updatedAt);

    boolean deleteByIdAndUserId(String id, String userId);
}
