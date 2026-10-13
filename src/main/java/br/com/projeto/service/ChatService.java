package br.com.projeto.service;

import br.com.projeto.exception.ValidationException;
import br.com.projeto.model.ChatReply;
import br.com.projeto.model.Conversation;
import br.com.projeto.model.Message;
import br.com.projeto.model.MessageRole;
import br.com.projeto.rag.RetrievalResult;
import br.com.projeto.repository.MessageRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluxo de envio de mensagem:
 * ChatController → ChatService → RAGService → GroqService → Groq API → MongoDB → JavaFX
 */
public class ChatService {

    public static final int MAX_MESSAGE_LENGTH = 4000;
    /** Quantidade de mensagens anteriores enviadas como memória da conversa. */
    private static final int HISTORY_SIZE = 10;
    private static final int AUTO_TITLE_LENGTH = 40;

    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final RAGService ragService;
    private final AiClient aiClient;
    private final SessionManager session;

    public ChatService(ConversationService conversationService, MessageRepository messageRepository,
                       RAGService ragService, AiClient aiClient, SessionManager session) {
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.ragService = ragService;
        this.aiClient = aiClient;
        this.session = session;
    }

    public ChatReply send(String conversationId, String text) {
        String userId = session.requireUserId();
        String question = text == null ? "" : text.trim();
        if (question.isEmpty()) {
            throw new ValidationException("Digite uma mensagem antes de enviar.");
        }
        if (question.length() > MAX_MESSAGE_LENGTH) {
            throw new ValidationException("A mensagem deve ter no máximo " + MAX_MESSAGE_LENGTH + " caracteres.");
        }

        // garante que a conversa existe e pertence ao usuário logado
        Conversation conversation = conversationService.getMine(conversationId);
        List<Message> history = messageRepository.findByConversation(conversation.getId(), userId);

        RetrievalResult retrieval = ragService.retrieve(question);
        List<AiMessage> prompt = new ArrayList<>();
        prompt.add(new AiMessage(MessageRole.SYSTEM, ragService.buildSystemPrompt(retrieval)));
        history.stream()
                .skip(Math.max(0, history.size() - HISTORY_SIZE))
                .forEach(m -> prompt.add(new AiMessage(m.getRole(), m.getContent())));
        prompt.add(new AiMessage(MessageRole.USER, question));

        // a IA é chamada antes de persistir: se falhar, nada fica pela metade no histórico
        String answer = aiClient.complete(prompt);

        Message userMessage = messageRepository.save(
                new Message(conversation.getId(), userId, MessageRole.USER, question));
        Message assistantMessage = new Message(conversation.getId(), userId, MessageRole.ASSISTANT, answer);
        assistantMessage.setCreatedAt(Instant.now().plusMillis(1));
        assistantMessage.setSources(retrieval.sourceTitles());
        messageRepository.save(assistantMessage);

        if (conversation.hasDefaultTitle()) {
            conversation = conversationService.rename(conversation.getId(), autoTitle(question));
        } else {
            conversationService.touch(conversation.getId());
        }
        return new ChatReply(userMessage, assistantMessage, conversation);
    }

    /** Usa o início da primeira pergunta como título da conversa. */
    static String autoTitle(String question) {
        String singleLine = question.replaceAll("\\s+", " ").trim();
        if (singleLine.length() <= AUTO_TITLE_LENGTH) {
            return singleLine;
        }
        int cut = singleLine.lastIndexOf(' ', AUTO_TITLE_LENGTH);
        return singleLine.substring(0, cut > 15 ? cut : AUTO_TITLE_LENGTH) + "…";
    }
}
