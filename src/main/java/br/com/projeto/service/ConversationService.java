package br.com.projeto.service;

import br.com.projeto.model.Conversation;
import br.com.projeto.model.Message;
import br.com.projeto.repository.ConversationRepository;
import br.com.projeto.repository.MessageRepository;
import br.com.projeto.util.AppException;
import br.com.projeto.util.Security;

import java.time.Instant;
import java.util.List;

/**
 * Criar, listar, abrir, renomear e excluir conversas. O dono vem sempre da sessão e o
 * repositório filtra cada operação por (id da conversa + id do usuário).
 */
public class ConversationService {

    private static final String NOT_FOUND = "Conversa não encontrada. Ela pode ter sido excluída.";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AuthenticationService auth;

    public ConversationService(ConversationRepository conversationRepository, MessageRepository messageRepository,
                               AuthenticationService auth) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.auth = auth;
    }

    public Conversation create() {
        return conversationRepository.save(new Conversation(auth.requireUserId(), Conversation.DEFAULT_TITLE));
    }

    public List<Conversation> listMine() {
        return conversationRepository.findByUserId(auth.requireUserId());
    }

    public Conversation getMine(String conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, auth.requireUserId())
                .orElseThrow(() -> new AppException(NOT_FOUND));
    }

    public List<Message> messagesOf(String conversationId) {
        return messageRepository.findByConversation(getMine(conversationId).getId(), auth.requireUserId());
    }

    public Conversation rename(String conversationId, String newTitle) {
        String title = Security.title(newTitle);
        if (!conversationRepository.update(conversationId, auth.requireUserId(), title, Instant.now())) {
            throw new AppException(NOT_FOUND);
        }
        return getMine(conversationId);
    }

    public void delete(String conversationId) {
        String userId = auth.requireUserId();
        Conversation conversation = getMine(conversationId);
        messageRepository.deleteByConversation(conversation.getId(), userId);
        if (!conversationRepository.deleteByIdAndUserId(conversation.getId(), userId)) {
            throw new AppException("Não foi possível excluir a conversa: ela não foi encontrada.");
        }
    }

    /** Atualiza a data da conversa (ela sobe para o topo da lista). */
    void touch(String conversationId) {
        conversationRepository.update(conversationId, auth.requireUserId(), null, Instant.now());
    }
}
