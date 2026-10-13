package br.com.projeto.service;

import br.com.projeto.exception.NotFoundException;
import br.com.projeto.model.Conversation;
import br.com.projeto.model.Message;
import br.com.projeto.repository.ConversationRepository;
import br.com.projeto.repository.MessageRepository;
import br.com.projeto.util.Validator;

import java.time.Instant;
import java.util.List;

/**
 * Gerenciamento das conversas. O proprietário vem sempre da sessão, e cada operação
 * é filtrada por (id da conversa + id do usuário) no repositório.
 */
public class ConversationService {

    private static final String NOT_FOUND = "Conversa não encontrada. Ela pode ter sido excluída.";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SessionManager session;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               SessionManager session) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.session = session;
    }

    public Conversation create() {
        return conversationRepository.save(new Conversation(session.requireUserId(), Conversation.DEFAULT_TITLE));
    }

    public List<Conversation> listMine() {
        return conversationRepository.findByUserId(session.requireUserId());
    }

    public Conversation getMine(String conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, session.requireUserId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    public List<Message> messagesOf(String conversationId) {
        Conversation conversation = getMine(conversationId);
        return messageRepository.findByConversation(conversation.getId(), session.requireUserId());
    }

    public Conversation rename(String conversationId, String newTitle) {
        String title = Validator.title(newTitle);
        Instant now = Instant.now();
        if (!conversationRepository.updateTitle(conversationId, session.requireUserId(), title, now)) {
            throw new NotFoundException(NOT_FOUND);
        }
        return getMine(conversationId);
    }

    public void delete(String conversationId) {
        String userId = session.requireUserId();
        Conversation conversation = getMine(conversationId);
        messageRepository.deleteByConversation(conversation.getId(), userId);
        if (!conversationRepository.deleteByIdAndUserId(conversation.getId(), userId)) {
            throw new NotFoundException("Não foi possível excluir a conversa: ela não foi encontrada.");
        }
    }

    void touch(String conversationId) {
        conversationRepository.touch(conversationId, session.requireUserId(), Instant.now());
    }
}
