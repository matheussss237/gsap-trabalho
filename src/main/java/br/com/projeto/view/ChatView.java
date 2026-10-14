package br.com.projeto.view;

import br.com.projeto.config.AppConfig;
import br.com.projeto.model.Conversation;
import br.com.projeto.service.ChatService;
import br.com.projeto.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface principal: sidebar (nova conversa, busca, lista, base de conhecimento, usuário,
 * tema e logout) + área de chat (cabeçalho, mensagens e campo de envio).
 */
public class ChatView {

    public static final List<String> SUGGESTIONS = List.of(
            "Qual é a política de home office da empresa?",
            "Explique encapsulamento em Java",
            "Como funciona o RAG?",
            "Como abrir um chamado no suporte técnico?");

    // sidebar
    public final VBox sidebar = new VBox(12);
    public final Button newConversationButton = new Button("Nova conversa", Components.icon(Components.PLUS, 14));
    public final TextField searchField = new TextField();
    public final ListView<Conversation> conversationList = new ListView<>();
    public final Button knowledgeButton = new Button("Base de conhecimento", Components.icon(Components.BOOK, 14));
    public final Button themeButton = Components.iconButton(Components.SUN, "Alternar tema claro/escuro");
    public final Button logoutButton = Components.iconButton(Components.LOGOUT, "Sair da conta");

    // chat
    public final Button toggleSidebarButton = Components.iconButton(Components.MENU, "Mostrar/ocultar conversas");
    public final Label conversationTitle = Components.label("Nova conversa", "chat-title");
    public final Label conversationSubtitle = Components.label("", "chat-subtitle");
    public final Button renameButton = Components.iconButton(Components.EDIT, "Renomear conversa");
    public final Button deleteButton = Components.iconButton(Components.TRASH, "Excluir conversa");
    public final VBox messages = new VBox(22);
    public final ScrollPane messagesScroll = new ScrollPane(messages);
    public final Label greeting = Components.label("", "empty-title");
    public final FlowPane suggestions = new FlowPane(10, 10);
    public final Components.TypingIndicator typing = new Components.TypingIndicator();
    public final Components.Banner banner = new Components.Banner();
    public final TextArea input = new TextArea();
    public final Button sendButton = new Button(null, Components.icon(Components.SEND, 16));
    public final Label counter = Components.label("", "composer-hint");

    /** Ações dos botões de cada conversa (definidas pelo controller). */
    public Consumer<Conversation> onRename = c -> { };
    public Consumer<Conversation> onDelete = c -> { };

    private final BorderPane root = new BorderPane();
    private final VBox emptyState;
    private final StackPane loadingOverlay;

    public ChatView(String userName, String userEmail) {
        root.getStyleClass().add("chat-root");
        buildSidebar(userName, userEmail);

        // cabeçalho
        VBox titles = new VBox(1, conversationTitle, conversationSubtitle);
        titles.setMinWidth(0);
        HBox.setHgrow(titles, Priority.ALWAYS);
        deleteButton.getStyleClass().add("danger-action");
        HBox header = new HBox(12, toggleSidebarButton, titles, renameButton, deleteButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("chat-header");

        // mensagens
        messages.setPadding(new Insets(28, 32, 28, 32));
        messagesScroll.setFitToWidth(true);
        messagesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagesScroll.getStyleClass().add("transparent-scroll");
        messages.heightProperty().addListener((obs, old, h) -> messagesScroll.setVvalue(1.0)); // rola até o fim

        Label subtitle = Components.label("Pergunte qualquer coisa. Quando a pergunta tiver relação com a base de "
                + "conhecimento, a Lumi usa esses documentos e mostra as fontes.", "empty-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(520);
        subtitle.setStyle("-fx-text-alignment: center;");
        suggestions.setAlignment(Pos.CENTER);
        suggestions.setMaxWidth(640);
        emptyState = new VBox(16, Components.logo(72), greeting, subtitle, suggestions);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(24));

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(36, 36);
        loadingOverlay = new StackPane(new VBox(10, spinner, Components.label("Carregando conversa…", "muted")));
        ((VBox) loadingOverlay.getChildren().get(0)).setAlignment(Pos.CENTER);
        loadingOverlay.getStyleClass().add("loading-overlay");
        loadingOverlay.setVisible(false);
        StackPane chatArea = new StackPane(messagesScroll, emptyState, loadingOverlay);
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        // campo de envio
        input.setPromptText("Pergunte qualquer coisa à Lumi…  (Enter envia, Shift+Enter quebra linha)");
        input.setWrapText(true);
        input.setPrefRowCount(2);
        input.getStyleClass().add("composer-input");
        HBox.setHgrow(input, Priority.ALWAYS);
        sendButton.getStyleClass().add("send-button");
        HBox composer = new HBox(10, input, sendButton);
        composer.setAlignment(Pos.BOTTOM_RIGHT);
        composer.getStyleClass().add("composer");
        Label ragHint = Components.label("RAG ativo · respostas usam a base de conhecimento quando relevante",
                "composer-hint");
        ragHint.setGraphic(Components.icon(Components.BOOK, 11));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox hints = new HBox(ragHint, spacer, counter);
        input.textProperty().addListener((obs, o, text) -> updateCounter(text.length()));
        updateCounter(0);
        VBox bottom = new VBox(8, typing, banner, composer, hints);
        bottom.getStyleClass().add("composer-area");

        sidebar.managedProperty().bind(sidebar.visibleProperty());
        root.setLeft(sidebar);
        root.setCenter(new VBox(header, chatArea, bottom));
        // janela estreita: a sidebar recolhe sozinha (o botão ☰ reabre)
        root.widthProperty().addListener((obs, old, w) -> {
            if (w.doubleValue() < 820 && old.doubleValue() >= 820) sidebar.setVisible(false);
            if (w.doubleValue() >= 820 && old.doubleValue() < 820) sidebar.setVisible(true);
        });
    }

    private void buildSidebar(String userName, String userEmail) {
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(280);
        sidebar.setMinWidth(240);

        HBox header = new HBox(10, Components.logo(30), Components.label(AppConfig.APP_NAME, "sidebar-title"));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 4, 6, 4));

        newConversationButton.getStyleClass().add("new-chat-button");
        newConversationButton.setMaxWidth(Double.MAX_VALUE);
        searchField.setPromptText("Buscar conversas");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox search = new HBox(8, Components.icon(Components.SEARCH, 14), searchField);
        search.setAlignment(Pos.CENTER_LEFT);
        search.getStyleClass().add("search-box");

        conversationList.setCellFactory(list -> new ConversationCell());
        Label empty = Components.label("Nenhuma conversa ainda.\nClique em \"Nova conversa\" para começar.", "empty-list");
        empty.setWrapText(true);
        conversationList.setPlaceholder(empty);
        VBox.setVgrow(conversationList, Priority.ALWAYS);

        knowledgeButton.getStyleClass().add("sidebar-link");
        knowledgeButton.setMaxWidth(Double.MAX_VALUE);
        knowledgeButton.setAlignment(Pos.CENTER_LEFT);
        logoutButton.getStyleClass().add("logout-button");

        VBox userInfo = new VBox(1, Components.label(userName, "user-name"), Components.label(userEmail, "user-email"));
        userInfo.setMinWidth(0);
        HBox.setHgrow(userInfo, Priority.ALWAYS);
        HBox userCard = new HBox(10, Components.avatar(userName, 34), userInfo, themeButton, logoutButton);
        userCard.setAlignment(Pos.CENTER_LEFT);
        userCard.getStyleClass().add("user-card");

        sidebar.getChildren().addAll(header, newConversationButton, search, Components.label("CONVERSAS", "section-title"),
                conversationList, knowledgeButton, userCard);
    }

    private void updateCounter(int length) {
        counter.setText(length + "/" + ChatService.MAX_MESSAGE_LENGTH);
        counter.getStyleClass().remove("over-limit");
        if (length > ChatService.MAX_MESSAGE_LENGTH) counter.getStyleClass().add("over-limit");
    }

    public void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        messagesScroll.setVisible(!show);
    }

    public void setLoading(boolean loading) {
        loadingOverlay.setVisible(loading);
    }

    public void addMessage(Node node) {
        messages.getChildren().add(node);
        showEmptyState(false);
    }

    public Parent root() {
        return root;
    }

    /** Célula da lista: título, data e botões de renomear/excluir (também no menu de contexto). */
    private class ConversationCell extends ListCell<Conversation> {

        private final Label title = Components.label("", "conversation-title");
        private final Label date = Components.label("", "conversation-date");
        private final HBox content;
        private final ContextMenu menu;

        ConversationCell() {
            setPrefWidth(0); // acompanha a largura da lista (sem barra horizontal)
            Button rename = Components.iconButton(Components.EDIT, "Renomear");
            Button delete = Components.iconButton(Components.TRASH, "Excluir");
            delete.getStyleClass().add("danger-action");
            rename.getStyleClass().add("cell-action");
            delete.getStyleClass().add("cell-action");
            HBox actions = new HBox(2, rename, delete);
            actions.visibleProperty().bind(hoverProperty().or(selectedProperty()));
            VBox texts = new VBox(2, title, date);
            texts.setMinWidth(0);
            HBox.setHgrow(texts, Priority.ALWAYS);
            content = new HBox(10, Components.icon(Components.CHAT, 14), texts, actions);
            content.setAlignment(Pos.CENTER_LEFT);

            rename.setOnAction(e -> onRename.accept(getItem()));
            delete.setOnAction(e -> onDelete.accept(getItem()));
            MenuItem renameItem = new MenuItem("Renomear");
            renameItem.setOnAction(e -> onRename.accept(getItem()));
            MenuItem deleteItem = new MenuItem("Excluir");
            deleteItem.setOnAction(e -> onDelete.accept(getItem()));
            menu = new ContextMenu(renameItem, deleteItem);
        }

        @Override
        protected void updateItem(Conversation item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty || item == null) {
                setGraphic(null);
                setContextMenu(null);
            } else {
                title.setText(item.getTitle());
                date.setText(Ui.formatDate(item.getUpdatedAt()));
                setGraphic(content);
                setContextMenu(menu);
            }
        }
    }
}
