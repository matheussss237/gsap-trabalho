package br.com.projeto.controller;

import br.com.projeto.model.ChatReply;
import br.com.projeto.model.Conversation;
import br.com.projeto.model.Message;
import br.com.projeto.model.MessageRole;
import br.com.projeto.service.ChatService;
import br.com.projeto.service.ConversationService;
import br.com.projeto.service.SessionManager;
import br.com.projeto.util.DateFormat;
import br.com.projeto.util.FxAsync;
import br.com.projeto.view.ChatView;
import br.com.projeto.view.components.MessageBubble;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.List;

/**
 * Controla a área de conversa. Encaminha o envio para o ChatService
 * (que aciona RAGService → GroqService) e atualiza a interface com a resposta.
 */
public class ChatController {

    private final ChatView view;
    private final ChatService chatService;
    private final ConversationService conversationService;
    private final SessionManager session;
    private final SidebarController sidebar;

    private Conversation current;
    private boolean sending;

    public ChatController(ChatView view, ChatService chatService, ConversationService conversationService,
                          SessionManager session, SidebarController sidebar) {
        this.view = view;
        this.chatService = chatService;
        this.conversationService = conversationService;
        this.session = session;
        this.sidebar = sidebar;

        session.currentUser().ifPresent(user -> {
            view.sidebar().setUser(user.getName(), user.getEmail());
            view.setGreeting("Olá, " + firstName(user.getName()) + "! Como posso ajudar?");
        });
        for (String suggestion : ChatView.SUGGESTIONS) {
            view.addSuggestion(suggestion).setOnAction(e -> {
                view.input().setText(suggestion);
                send();
            });
        }

        view.sendButton().setOnAction(e -> send());
        view.input().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
        view.toggleSidebarButton().setOnAction(e -> view.sidebar().setVisible(!view.sidebar().isVisible()));
        view.renameButton().setOnAction(e -> sidebar.rename(current));
        view.deleteButton().setOnAction(e -> sidebar.delete(current));

        sidebar.setOnSelect(this::open);
        sidebar.setOnRenamed(this::onRenamed);
        sidebar.setOnDeleted(this::onDeleted);
        sidebar.setOnError(message -> view.banner().error(message));
        open(null);
    }

    /** Enter envia; Shift+Enter insere quebra de linha. */
    private void handleKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            event.consume();
            if (event.isShiftDown()) {
                view.input().insertText(view.input().getCaretPosition(), "\n");
            } else {
                send();
            }
        }
    }

    /** Abre uma conversa e carrega o histórico salvo no MongoDB. */
    public void open(Conversation conversation) {
        current = conversation;
        view.banner().hide();
        view.clearMessages();
        updateHeader();
        if (conversation == null) {
            view.showEmptyState(true);
            return;
        }
        view.setLoading(true);
        String requestedId = conversation.getId();
        FxAsync.run(() -> conversationService.messagesOf(requestedId), messages -> {
            if (current == null || !current.getId().equals(requestedId)) {
                return; // o usuário já trocou de conversa
            }
            view.setLoading(false);
            render(messages);
        }, error -> {
            view.setLoading(false);
            view.showEmptyState(false);
            view.banner().error(error);
        });
        view.input().requestFocus();
    }

    private void render(List<Message> messages) {
        view.clearMessages();
        if (messages.isEmpty()) {
            view.showEmptyState(true);
            return;
        }
        for (Message message : messages) {
            view.addMessageNode(bubble(message));
        }
    }

    private void send() {
        String text = view.input().getText();
        if (sending || text == null || text.isBlank()) {
            return;
        }
        if (text.length() > ChatService.MAX_MESSAGE_LENGTH) {
            view.banner().error("A mensagem deve ter no máximo " + ChatService.MAX_MESSAGE_LENGTH + " caracteres.");
            return;
        }
        setSending(true);
        view.banner().hide();

        // exibe a pergunta imediatamente (será confirmada quando a resposta chegar)
        String userId = session.currentUser().map(u -> u.getId()).orElse(null);
        Message optimistic = new Message(current == null ? null : current.getId(), userId, MessageRole.USER, text.trim());
        MessageBubble pending = bubble(optimistic);
        pending.markPending(true);
        view.addMessageNode(pending);
        view.input().clear();

        Conversation target = current;
        FxAsync.run(() -> {
            Conversation conversation = target != null ? target : conversationService.create();
            return chatService.send(conversation.getId(), text);
        }, reply -> onReply(reply, pending), error -> {
            setSending(false);
            view.removeMessageNode(pending);
            if (current == target) {
                view.input().setText(text);
                view.input().positionCaret(text.length());
            }
            view.banner().error(error);
        });
    }

    private void onReply(ChatReply reply, MessageBubble pending) {
        setSending(false);
        Conversation conversation = reply.conversation();
        boolean stillHere = current == null || current.getId().equals(conversation.getId());
        sidebar.upsert(conversation);
        if (!stillHere) {
            return; // resposta de uma conversa que não está aberta: já está salva no histórico
        }
        current = conversation;
        updateHeader();
        pending.markPending(false);
        view.addMessageNode(bubble(reply.assistantMessage()));
        view.input().requestFocus();
    }

    private void setSending(boolean value) {
        sending = value;
        view.sendButton().setDisable(value);
        view.typingIndicator().setVisible(value);
        if (value) {
            view.typingIndicator().start();
        } else {
            view.typingIndicator().stop();
        }
    }

    private MessageBubble bubble(Message message) {
        String name = session.currentUser().map(u -> u.getName()).orElse("");
        MessageBubble bubble = new MessageBubble(message, name, view.messagesScroll().widthProperty());
        bubble.animateIn();
        return bubble;
    }

    private void onRenamed(Conversation conversation) {
        if (current != null && current.getId().equals(conversation.getId())) {
            current = conversation;
            updateHeader();
        }
    }

    private void onDeleted(Conversation conversation) {
        if (current != null && current.getId().equals(conversation.getId())) {
            open(null);
        }
    }

    private void updateHeader() {
        boolean hasConversation = current != null;
        view.conversationTitle().setText(hasConversation ? current.getTitle() : "Nova conversa");
        view.conversationSubtitle().setText(hasConversation
                ? "Criada em " + DateFormat.smart(current.getCreatedAt())
                : "Envie uma mensagem para iniciar uma conversa");
        view.renameButton().setDisable(!hasConversation);
        view.deleteButton().setDisable(!hasConversation);
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) return "";
        return name.trim().split("\\s+")[0];
    }
}
