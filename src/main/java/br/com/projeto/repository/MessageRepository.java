package br.com.projeto.repository;

import br.com.projeto.model.Message;

import java.util.List;

/** Acesso aos dados da coleção "messages". Sempre filtrado por conversa E usuário. */
public interface MessageRepository {

    Message save(Message message);

    List<Message> findByConversation(String conversationId, String userId);

    long deleteByConversation(String conversationId, String userId);
}
