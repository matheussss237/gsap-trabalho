package br.com.projeto.controller;

import br.com.projeto.Main;
import br.com.projeto.model.Conversation;
import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.model.Message;
import br.com.projeto.model.User;
import br.com.projeto.rag.TextProcessor;
import br.com.projeto.service.AuthenticationService;
import br.com.projeto.service.ChatService;
import br.com.projeto.service.ConversationService;
import br.com.projeto.service.RAGService;
import br.com.projeto.util.Ui;
import br.com.projeto.view.ChatView;
import br.com.projeto.view.Components;
import br.com.projeto.view.KnowledgeView;
import br.com.projeto.view.MessageView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Controla a interface principal: sidebar (listar, criar, buscar, renomear, excluir conversas,
 * logout), área de chat (envio pelo ChatService → RAGService → GroqService) e a janela da base
 * de conhecimento.
 */
public class ChatController {

    private final Main app;
    private final ChatView view;
    private final AuthenticationService auth;
    private final ConversationService conversations;
    private final ChatService chat;
    private final RAGService rag;
    private final User user;

    private final ObservableList<Conversation> items = FXCollections.observableArrayList();
    private final FilteredList<Conversation> filtered = new FilteredList<>(items, c -> true);
    private Conversation current;
    private boolean sending;
    private boolean ignoreSelection;

    public ChatController(Main app, AuthenticationService auth, ConversationService conversations, ChatService chat,
                          RAGService rag, ThemeController theme) {
        this.app = app;
        this.auth = auth;
        this.conversations = conversations;
        this.chat = chat;
        this.rag = rag;
        this.user = auth.requireUser();
        this.view = new ChatView(user.getName(), user.getEmail());

        theme.bindToggleButton(view.themeButton);
        view.greeting.setText("Olá, " + user.firstName() + "! Como posso ajudar?");
        for (String suggestion : ChatView.SUGGESTIONS) {
            Button chip = new Button(suggestion, Components.icon(Components.SPARK, 12));
            chip.getStyleClass().add("suggestion-chip");
            chip.setOnAction(e -> {
                view.input.setText(suggestion);
                send();
            });
            view.suggestions.getChildren().add(chip);
        }

        // sidebar
        view.conversationList.setItems(filtered);
        view.conversationList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (!ignoreSelection && selected != null && (old == null || !selected.getId().equals(old.getId()))) {
                open(selected);
            }
        });
        view.searchField.textProperty().addListener((obs, old, text) -> {
            String term = text.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(c -> term.isEmpty() || c.getTitle().toLowerCase(Locale.ROOT).contains(term));
        });
        view.newConversationButton.setOnAction(e -> createConversation());
        view.onRename = this::rename;
        view.onDelete = this::delete;
        view.knowledgeButton.setOnAction(e -> openKnowledgeBase());
        view.logoutButton.setOnAction(e -> {
            auth.logout();
            app.showLogin(null, "Você saiu da sua conta.");
        });

        // chat
        view.sendButton.setOnAction(e -> send());
        view.input.addEventFilter(KeyEvent.KEY_PRESSED, this::handleEnter);
        view.toggleSidebarButton.setOnAction(e -> view.sidebar.setVisible(!view.sidebar.isVisible()));
        view.renameButton.setOnAction(e -> rename(current));
        view.deleteButton.setOnAction(e -> delete(current));

        open(null);
        loadConversations();
    }

    public ChatView view() {
        return view;
    }

    // ---------- conversas ----------

    /** Carrega apenas as conversas do usuário logado. */
    private void loadConversations() {
        Ui.async(conversations::listMine, list -> {
            items.setAll(list);
            if (!list.isEmpty()) view.conversationList.getSelectionModel().selectFirst();
        }, view.banner::error);
    }

    private void createConversation() {
        view.newConversationButton.setDisable(true);
        Ui.async(conversations::create, created -> {
            view.newConversationButton.setDisable(false);
            view.searchField.clear();
            items.add(0, created);
            view.conversationList.getSelectionModel().select(created);
            view.conversationList.scrollTo(0);
        }, error -> {
            view.newConversationButton.setDisable(false);
            view.banner.error(error);
        });
    }

    private void rename(Conversation conversation) {
        if (conversation == null) return;
        Ui.input(window(), "Renomear conversa", "Novo título:", conversation.getTitle())
                .filter(title -> !title.isBlank() && !title.equals(conversation.getTitle()))
                .ifPresent(title -> Ui.async(() -> conversations.rename(conversation.getId(), title), updated -> {
                    replace(updated, false);
                    if (current != null && current.getId().equals(updated.getId())) {
                        current = updated;
                        updateHeader();
                    }
                }, error -> Ui.error(window(), "Não foi possível renomear", error)));
    }

    private void delete(Conversation conversation) {
        if (conversation == null) return;
        if (!Ui.confirm(window(), "Excluir conversa?", "A conversa \"" + conversation.getTitle()
                + "\" e todas as suas mensagens serão excluídas definitivamente.", "Excluir")) {
            return;
        }
        Ui.async(() -> {
            conversations.delete(conversation.getId());
            return conversation;
        }, deleted -> {
            items.removeIf(c -> c.getId().equals(deleted.getId()));
            if (current != null && current.getId().equals(deleted.getId())) {
                open(null);
                if (!items.isEmpty()) view.conversationList.getSelectionModel().selectFirst();
            }
        }, error -> Ui.error(window(), "Erro ao excluir conversa", error));
    }

    /** Atualiza a conversa na lista (moveToTop = foi usada agora) sem recarregar o chat. */
    private void replace(Conversation conversation, boolean moveToTop) {
        ignoreSelection = true;
        Conversation selected = view.conversationList.getSelectionModel().getSelectedItem();
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(conversation.getId())) index = i;
        }
        if (index >= 0 && !moveToTop) {
            items.set(index, conversation);
        } else {
            if (index >= 0) items.remove(index);
            items.add(0, conversation);
        }
        if (selected == null || selected.getId().equals(conversation.getId())) {
            view.conversationList.getSelectionModel().select(conversation);
        }
        ignoreSelection = false;
    }

    // ---------- chat ----------

    /** Abre uma conversa e carrega o histórico salvo no MongoDB. */
    private void open(Conversation conversation) {
        current = conversation;
        view.banner.hide();
        view.messages.getChildren().clear();
        updateHeader();
        if (conversation == null) {
            view.showEmptyState(true);
            return;
        }
        view.setLoading(true);
        String id = conversation.getId();
        Ui.async(() -> conversations.messagesOf(id), messages -> {
            if (current == null || !current.getId().equals(id)) return; // já trocou de conversa
            view.setLoading(false);
            view.messages.getChildren().clear();
            view.showEmptyState(messages.isEmpty());
            messages.forEach(m -> view.addMessage(bubble(m)));
        }, error -> {
            view.setLoading(false);
            view.showEmptyState(false);
            view.banner.error(error);
        });
        view.input.requestFocus();
    }

    /** Enter envia; Shift+Enter quebra linha. */
    private void handleEnter(KeyEvent event) {
        if (event.getCode() != KeyCode.ENTER) return;
        event.consume();
        if (event.isShiftDown()) {
            view.input.insertText(view.input.getCaretPosition(), "\n");
        } else {
            send();
        }
    }

    private void send() {
        String text = view.input.getText();
        if (sending || text == null || text.isBlank()) return;
        if (text.length() > ChatService.MAX_MESSAGE_LENGTH) {
            view.banner.error("A mensagem deve ter no máximo " + ChatService.MAX_MESSAGE_LENGTH + " caracteres.");
            return;
        }
        setSending(true);
        view.banner.hide();

        // mostra a pergunta na hora; se a IA falhar, ela é removida e o texto volta para o campo
        MessageView pending = bubble(new Message(null, user.getId(), Message.Role.USER, text.trim()));
        pending.setOpacity(0.75);
        view.addMessage(pending);
        view.input.clear();

        Conversation target = current;
        Ui.async(() -> chat.send((target != null ? target : conversations.create()).getId(), text), reply -> {
            setSending(false);
            replace(reply.conversation(), true);
            if (current != null && !current.getId().equals(reply.conversation().getId())) return; // usuário trocou de conversa
            current = reply.conversation();
            updateHeader();
            pending.setOpacity(1);
            view.addMessage(bubble(reply.answer()));
            view.input.requestFocus();
        }, error -> {
            setSending(false);
            view.messages.getChildren().remove(pending);
            if (current == target) view.input.setText(text);
            view.banner.error(error);
        });
    }

    private void setSending(boolean value) {
        sending = value;
        view.sendButton.setDisable(value);
        view.typing.setActive(value);
    }

    private MessageView bubble(Message message) {
        MessageView bubble = new MessageView(message, user.getName(), view.messagesScroll.widthProperty());
        bubble.animateIn();
        return bubble;
    }

    private void updateHeader() {
        boolean has = current != null;
        view.conversationTitle.setText(has ? current.getTitle() : "Nova conversa");
        view.conversationSubtitle.setText(has ? "Criada em " + Ui.formatDate(current.getCreatedAt())
                : "Envie uma mensagem para iniciar uma conversa");
        view.renameButton.setDisable(!has);
        view.deleteButton.setDisable(!has);
    }

    private Window window() {
        return view.root().getScene().getWindow();
    }

    // ---------- base de conhecimento ----------

    private void openKnowledgeBase() {
        KnowledgeView kb = new KnowledgeView();
        Stage stage = app.openWindow(kb.root(), "Base de conhecimento", 980, 640);
        kb.closeButton.setOnAction(e -> stage.close());
        kb.newDocumentButton.setOnAction(e -> {
            kb.documentList.getSelectionModel().clearSelection();
            kb.showNewDocument();
        });
        kb.documentList.getSelectionModel().selectedItemProperty().addListener((obs, old, doc) -> {
            if (doc != null) kb.showDocument(doc, Objects.equals(doc.getOwnerId(), user.getId()));
        });
        kb.saveButton.setOnAction(e -> saveDocument(kb));
        kb.deleteButton.setOnAction(e -> deleteDocument(kb, stage));
        kb.searchButton.setOnAction(e -> testRetrieval(kb));
        kb.queryField.setOnAction(e -> testRetrieval(kb));
        loadDocuments(kb, null);
        stage.showAndWait();
    }

    private void loadDocuments(KnowledgeView kb, String selectId) {
        Ui.async(rag::listDocuments, docs -> {
            kb.documentList.getItems().setAll(docs);
            docs.stream().filter(d -> d.getId().equals(selectId)).findFirst()
                    .ifPresent(d -> kb.documentList.getSelectionModel().select(d));
        }, kb.banner::error);
    }

    private void saveDocument(KnowledgeView kb) {
        if (kb.title.value().isBlank()) {
            kb.title.showError("Informe o título.");
            return;
        }
        kb.saveButton.setLoading(true, "Salvando…");
        Ui.async(() -> rag.addDocument(kb.title.value(), kb.content.getText(), kb.keywords.value()), saved -> {
            kb.saveButton.setLoading(false, null);
            loadDocuments(kb, saved.getId());
            kb.banner.success("Documento salvo e indexado com " + saved.getKeywords().size() + " palavras-chave.");
        }, error -> {
            kb.saveButton.setLoading(false, null);
            kb.banner.error(error);
        });
    }

    private void deleteDocument(KnowledgeView kb, Stage stage) {
        KnowledgeDocument doc = kb.documentList.getSelectionModel().getSelectedItem();
        if (doc == null || !Ui.confirm(stage, "Excluir documento?",
                "O documento \"" + doc.getTitle() + "\" deixará de ser usado pelo RAG.", "Excluir")) {
            return;
        }
        Ui.async(() -> {
            rag.deleteDocument(doc.getId());
            return true;
        }, ok -> {
            kb.showNewDocument();
            loadDocuments(kb, null);
            kb.banner.success("Documento excluído.");
        }, kb.banner::error);
    }

    /** Executa o mesmo RAG do chat e mostra cada etapa. */
    private void testRetrieval(KnowledgeView kb) {
        String query = kb.queryField.getText();
        if (query == null || query.isBlank()) return;
        Set<String> removed = new LinkedHashSet<>();
        TextProcessor.tokenize(TextProcessor.normalize(query)).stream()
                .filter(TextProcessor::isStopWord).forEach(removed::add);

        kb.searchButton.setDisable(true);
        Ui.async(() -> rag.retrieve(query), result -> {
            kb.searchButton.setDisable(false);
            chips(kb.keywordChips.getChildren(), result.keywords(), "chip-keyword", "Nenhuma palavra relevante.");
            chips(kb.stopWordChips.getChildren(), List.copyOf(removed), "chip-stopword", "Nenhuma.");
            kb.results.getChildren().clear();
            if (!result.hasContext()) {
                kb.results.getChildren().add(muted("Nenhum documento encontrado. A IA responderá só com o "
                        + "conhecimento geral do modelo."));
            }
            int position = 1;
            for (RAGService.ScoredDocument scored : result.documents()) {
                String preview = scored.document().getContent();
                Label snippet = muted(preview.length() > 220 ? preview.substring(0, 220) + "…" : preview);
                VBox card = new VBox(4, Components.label(position++ + ". " + scored.document().getTitle(), "settings-strong"),
                        muted("Pontuação: " + scored.score() + "   ·   termos encontrados: "
                                + String.join(", ", scored.matchedTerms())), snippet);
                card.getStyleClass().add("result-card");
                kb.results.getChildren().add(card);
            }
        }, error -> {
            kb.searchButton.setDisable(false);
            kb.results.getChildren().setAll(muted(error));
        });
    }

    private void chips(List<javafx.scene.Node> target, List<String> words, String style, String emptyText) {
        target.clear();
        words.forEach(w -> target.add(KnowledgeView.chip(w, style)));
        if (words.isEmpty()) target.add(muted(emptyText));
    }

    private static Label muted(String text) {
        Label label = Components.label(text, "muted");
        label.setWrapText(true);
        return label;
    }
}
