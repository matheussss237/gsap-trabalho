package br.com.projeto.service;

import br.com.projeto.model.Conversation;
import br.com.projeto.model.Message;
import br.com.projeto.model.User;
import br.com.projeto.repository.MessageRepository;
import br.com.projeto.util.AppException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Envio de mensagens:
 * ChatController → ChatService → RAGService → GroqService → Groq API → MongoDB → JavaFX
 */
public class ChatService {

    public static final int MAX_MESSAGE_LENGTH = 4000;
    private static final int HISTORY_SIZE = 10; // mensagens anteriores enviadas como memória

    /** Pergunta e resposta já salvas, e a conversa atualizada. */
    public record Reply(Message question, Message answer, Conversation conversation) {
    }

    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final RAGService ragService;
    private final GroqService groqService;
    private final AuthenticationService auth;

    public ChatService(ConversationService conversationService, MessageRepository messageRepository,
                       RAGService ragService, GroqService groqService, AuthenticationService auth) {
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.ragService = ragService;
        this.groqService = groqService;
        this.auth = auth;
    }

    public Reply send(String conversationId, String text) {
        User user = auth.requireUser();
        String question = text == null ? "" : text.trim();
        if (question.isEmpty()) {
            throw new AppException("Digite uma mensagem antes de enviar.");
        }
        if (question.length() > MAX_MESSAGE_LENGTH) {
            throw new AppException("A mensagem deve ter no máximo " + MAX_MESSAGE_LENGTH + " caracteres.");
        }

        // garante que a conversa existe e pertence ao usuário logado
        Conversation conversation = conversationService.getMine(conversationId);
        List<Message> history = messageRepository.findByConversation(conversation.getId(), user.getId());

        RAGService.Result retrieval = ragService.retrieve(question);
        List<GroqService.Turn> prompt = new ArrayList<>();
        prompt.add(new GroqService.Turn(Message.Role.SYSTEM, ragService.buildSystemPrompt(retrieval, user.firstName())));
        history.stream()
                .skip(Math.max(0, history.size() - HISTORY_SIZE))
                .forEach(m -> prompt.add(new GroqService.Turn(m.getRole(), m.getContent())));
        prompt.add(new GroqService.Turn(Message.Role.USER, question));

        // a IA é chamada antes de salvar: se falhar, nada fica pela metade no histórico
        String answerText = groqService.complete(prompt);

        Message questionMessage = messageRepository.save(
                new Message(conversation.getId(), user.getId(), Message.Role.USER, question));
        Message answer = new Message(conversation.getId(), user.getId(), Message.Role.ASSISTANT, answerText);
        answer.setCreatedAt(Instant.now().plusMillis(1));
        answer.setSources(retrieval.sourceTitles());
        messageRepository.save(answer);

        if (conversation.hasDefaultTitle()) {
            conversation = conversationService.rename(conversation.getId(), autoTitle(question));
        } else {
            conversationService.touch(conversation.getId());
        }
        return new Reply(questionMessage, answer, conversation);
    }

    /** O início da primeira pergunta vira o título da conversa. */
    static String autoTitle(String question) {
        String line = question.replaceAll("\\s+", " ").trim();
        if (line.length() <= 40) {
            return line;
        }
        int cut = line.lastIndexOf(' ', 40);
        return line.substring(0, cut > 15 ? cut : 40) + "…";
    }
}
