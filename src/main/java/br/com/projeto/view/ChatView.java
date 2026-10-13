package br.com.projeto.view;

import br.com.projeto.service.ChatService;
import br.com.projeto.view.components.Icons;
import br.com.projeto.view.components.LogoMark;
import br.com.projeto.view.components.StatusBanner;
import br.com.projeto.view.components.TypingIndicator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Interface principal: sidebar + área de chat (cabeçalho, mensagens e campo de envio). */
public class ChatView {

    public static final List<String> SUGGESTIONS = List.of(
            "Qual é a política de home office da empresa?",
            "Explique encapsulamento em Java",
            "Como funciona o RAG?",
            "Como abrir um chamado no suporte técnico?");

    private final BorderPane root = new BorderPane();
    private final SidebarView sidebar = new SidebarView();

    private final Button toggleSidebarButton = new Button(null, Icons.create(Icons.MENU, 16));
    private final Label conversationTitle = new Label("Nova conversa");
    private final Label conversationSubtitle = new Label();
    private final Button renameButton = new Button(null, Icons.create(Icons.EDIT, 14));
    private final Button deleteButton = new Button(null, Icons.create(Icons.TRASH, 14));

    private final VBox messages = new VBox(22);
    private final ScrollPane messagesScroll = new ScrollPane(messages);
    private final VBox emptyState;
    private final Label greeting = new Label();
    private final FlowPane suggestions = new FlowPane(10, 10);
    private final StackPane chatArea;
    private final StackPane loadingOverlay;

    private final TypingIndicator typingIndicator = new TypingIndicator();
    private final StatusBanner banner = new StatusBanner();
    private final TextArea input = new TextArea();
    private final Button sendButton = new Button(null, Icons.create(Icons.SEND, 16));
    private final Label counter = new Label();

    public ChatView() {
        root.getStyleClass().add("chat-root");

        // ---- cabeçalho ----
        toggleSidebarButton.getStyleClass().add("icon-button");
        toggleSidebarButton.setTooltip(new Tooltip("Mostrar/ocultar conversas"));
        conversationTitle.getStyleClass().add("chat-title");
        conversationSubtitle.getStyleClass().add("chat-subtitle");
        VBox titles = new VBox(1, conversationTitle, conversationSubtitle);
        titles.setMinWidth(0);
        HBox.setHgrow(titles, Priority.ALWAYS);
        renameButton.getStyleClass().add("icon-button");
        renameButton.setTooltip(new Tooltip("Renomear conversa"));
        deleteButton.getStyleClass().addAll("icon-button", "danger-action");
        deleteButton.setTooltip(new Tooltip("Excluir conversa"));
        HBox header = new HBox(12, toggleSidebarButton, titles, renameButton, deleteButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("chat-header");

        // ---- mensagens ----
        messages.getStyleClass().add("messages");
        messages.setPadding(new Insets(28, 32, 28, 32));
        messagesScroll.setFitToWidth(true);
        messagesScroll.getStyleClass().add("messages-scroll");
        messagesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // rola automaticamente para a última mensagem
        messages.heightProperty().addListener((obs, old, h) -> messagesScroll.setVvalue(1.0));

        emptyState = buildEmptyState();
        loadingOverlay = buildLoadingOverlay();
        chatArea = new StackPane(messagesScroll, emptyState, loadingOverlay);
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        // ---- campo de envio ----
        input.setPromptText("Pergunte qualquer coisa ao Nexus…  (Enter envia, Shift+Enter quebra linha)");
        input.setWrapText(true);
        input.setPrefRowCount(2);
        input.getStyleClass().add("composer-input");
        HBox.setHgrow(input, Priority.ALWAYS);
        sendButton.getStyleClass().add("send-button");
        sendButton.setTooltip(new Tooltip("Enviar mensagem"));
        HBox composerBox = new HBox(10, input, sendButton);
        composerBox.setAlignment(Pos.BOTTOM_RIGHT);
        composerBox.getStyleClass().add("composer");

        Label ragHint = new Label("RAG ativo · respostas usam a base de conhecimento quando relevante",
                Icons.create(Icons.BOOK, 11));
        ragHint.getStyleClass().add("composer-hint");
        counter.getStyleClass().add("composer-hint");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox hints = new HBox(ragHint, spacer, counter);
        hints.setAlignment(Pos.CENTER_LEFT);
        input.textProperty().addListener((obs, o, text) -> updateCounter(text));
        updateCounter("");

        typingIndicator.managedProperty().bind(typingIndicator.visibleProperty());
        typingIndicator.setVisible(false);

        VBox bottom = new VBox(8, typingIndicator, banner, composerBox, hints);
        bottom.getStyleClass().add("composer-area");

        VBox main = new VBox(header, chatArea, bottom);
        main.getStyleClass().add("chat-main");

        sidebar.managedProperty().bind(sidebar.visibleProperty());
        root.setLeft(sidebar);
        root.setCenter(main);
        // em janelas estreitas a sidebar é recolhida automaticamente
        root.widthProperty().addListener((obs, old, w) -> {
            if (w.doubleValue() < 820 && old.doubleValue() >= 820) sidebar.setVisible(false);
            if (w.doubleValue() >= 820 && old.doubleValue() < 820) sidebar.setVisible(true);
        });
    }

    private VBox buildEmptyState() {
        greeting.getStyleClass().add("empty-title");
        Label subtitle = new Label("Pergunte qualquer coisa. Quando a sua pergunta tiver relação com a base de "
                + "conhecimento, o Nexus usa esses documentos e mostra as fontes.");
        subtitle.getStyleClass().add("empty-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(520);
        subtitle.setAlignment(Pos.CENTER);
        subtitle.setStyle("-fx-text-alignment: center;");
        suggestions.setAlignment(Pos.CENTER);
        suggestions.setMaxWidth(640);
        VBox box = new VBox(16, LogoMark.create(72), greeting, subtitle, suggestions);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state");
        box.setPadding(new Insets(24));
        box.managedProperty().bind(box.visibleProperty());
        return box;
    }

    private StackPane buildLoadingOverlay() {
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setMaxSize(36, 36);
        Label label = new Label("Carregando conversa…");
        label.getStyleClass().add("muted");
        VBox content = new VBox(10, spinner, label);
        content.setAlignment(Pos.CENTER);
        StackPane overlay = new StackPane(content);
        overlay.getStyleClass().add("loading-overlay");
        overlay.setVisible(false);
        return overlay;
    }

    private void updateCounter(String text) {
        int length = text == null ? 0 : text.length();
        counter.setText(length + "/" + ChatService.MAX_MESSAGE_LENGTH);
        counter.getStyleClass().remove("over-limit");
        if (length > ChatService.MAX_MESSAGE_LENGTH) {
            counter.getStyleClass().add("over-limit");
        }
    }

    public Button addSuggestion(String text) {
        Button chip = new Button(text, Icons.create(Icons.SPARK, 12));
        chip.getStyleClass().add("suggestion-chip");
        suggestions.getChildren().add(chip);
        return chip;
    }

    public void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        messagesScroll.setVisible(!show);
    }

    public void setLoading(boolean loading) {
        loadingOverlay.setVisible(loading);
    }

    public void addMessageNode(Node node) {
        messages.getChildren().add(node);
        showEmptyState(false);
    }

    public void removeMessageNode(Node node) {
        messages.getChildren().remove(node);
    }

    public void clearMessages() {
        messages.getChildren().clear();
    }

    public void setGreeting(String text) { greeting.setText(text); }

    public Parent root() { return root; }
    public SidebarView sidebar() { return sidebar; }
    public Button toggleSidebarButton() { return toggleSidebarButton; }
    public Label conversationTitle() { return conversationTitle; }
    public Label conversationSubtitle() { return conversationSubtitle; }
    public Button renameButton() { return renameButton; }
    public Button deleteButton() { return deleteButton; }
    public ScrollPane messagesScroll() { return messagesScroll; }
    public TypingIndicator typingIndicator() { return typingIndicator; }
    public StatusBanner banner() { return banner; }
    public TextArea input() { return input; }
    public Button sendButton() { return sendButton; }
}
