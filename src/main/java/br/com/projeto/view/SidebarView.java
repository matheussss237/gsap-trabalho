package br.com.projeto.view;

import br.com.projeto.config.AppConfig;
import br.com.projeto.model.Conversation;
import br.com.projeto.util.DateFormat;
import br.com.projeto.view.components.Icons;
import br.com.projeto.view.components.LogoMark;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Barra lateral: nova conversa, busca, lista de conversas e rodapé com usuário e opções. */
public class SidebarView extends VBox {

    private final Button newConversationButton = new Button("Nova conversa", Icons.create(Icons.PLUS, 14));
    private final TextField searchField = new TextField();
    private final ListView<Conversation> conversationList = new ListView<>();
    private final Label emptyLabel = new Label("Nenhuma conversa ainda.\nClique em \"Nova conversa\" para começar.");
    private final Button knowledgeButton = new Button("Base de conhecimento", Icons.create(Icons.BOOK, 14));
    private final Button settingsButton = new Button("Configurações", Icons.create(Icons.SETTINGS, 14));
    private final Button themeButton = new Button();
    private final Button logoutButton = new Button(null, Icons.create(Icons.LOGOUT, 15));
    private final Label userName = new Label();
    private final Label userEmail = new Label();
    private final Label avatarLetter = new Label();

    private Consumer<Conversation> onRename = c -> { };
    private Consumer<Conversation> onDelete = c -> { };

    public SidebarView() {
        getStyleClass().add("sidebar");
        setPrefWidth(280);
        setMinWidth(240);
        setSpacing(12);

        Label appName = new Label(AppConfig.APP_NAME);
        appName.getStyleClass().add("sidebar-title");
        HBox header = new HBox(10, LogoMark.create(30), appName);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("sidebar-header");

        newConversationButton.getStyleClass().add("new-chat-button");
        newConversationButton.setMaxWidth(Double.MAX_VALUE);

        searchField.setPromptText("Buscar conversas");
        searchField.getStyleClass().add("search-field");
        HBox search = new HBox(8, Icons.create(Icons.SEARCH, 14), searchField);
        search.getStyleClass().add("search-box");
        search.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label sectionTitle = new Label("CONVERSAS");
        sectionTitle.getStyleClass().add("section-title");

        conversationList.getStyleClass().add("conversation-list");
        conversationList.setCellFactory(list -> new ConversationCell());
        emptyLabel.getStyleClass().add("empty-list");
        emptyLabel.setWrapText(true);
        conversationList.setPlaceholder(emptyLabel);
        VBox.setVgrow(conversationList, Priority.ALWAYS);

        for (Button b : new Button[]{knowledgeButton, settingsButton}) {
            b.getStyleClass().add("sidebar-link");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
        }

        themeButton.getStyleClass().add("icon-button");
        themeButton.setTooltip(new Tooltip("Alternar tema claro/escuro"));
        logoutButton.getStyleClass().addAll("icon-button", "logout-button");
        logoutButton.setTooltip(new Tooltip("Sair da conta"));

        avatarLetter.getStyleClass().add("avatar-letter");
        StackPane avatar = new StackPane(avatarLetter);
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(34, 34);
        avatar.setMaxSize(34, 34);
        userName.getStyleClass().add("user-name");
        userEmail.getStyleClass().add("user-email");
        VBox userInfo = new VBox(1, userName, userEmail);
        userInfo.setMinWidth(0);
        HBox.setHgrow(userInfo, Priority.ALWAYS);
        HBox userCard = new HBox(10, avatar, userInfo, themeButton, logoutButton);
        userCard.setAlignment(Pos.CENTER_LEFT);
        userCard.getStyleClass().add("user-card");

        getChildren().addAll(header, newConversationButton, search, sectionTitle, conversationList,
                knowledgeButton, settingsButton, userCard);
    }

    public void setUser(String name, String email) {
        userName.setText(name);
        userEmail.setText(email);
        avatarLetter.setText(name == null || name.isBlank() ? "?" : name.substring(0, 1).toUpperCase());
    }

    public void setOnRename(Consumer<Conversation> handler) { this.onRename = handler; }
    public void setOnDelete(Consumer<Conversation> handler) { this.onDelete = handler; }

    public Button newConversationButton() { return newConversationButton; }
    public TextField searchField() { return searchField; }
    public ListView<Conversation> conversationList() { return conversationList; }
    public Button knowledgeButton() { return knowledgeButton; }
    public Button settingsButton() { return settingsButton; }
    public Button themeButton() { return themeButton; }
    public Button logoutButton() { return logoutButton; }

    /** Célula com título, data e botões de renomear/excluir (também via menu de contexto). */
    private class ConversationCell extends ListCell<Conversation> {

        private final Label title = new Label();
        private final Label date = new Label();
        private final Button rename = new Button(null, Icons.create(Icons.EDIT, 12));
        private final Button delete = new Button(null, Icons.create(Icons.TRASH, 12));
        private final HBox actions = new HBox(2, rename, delete);
        private final HBox root;
        private final ContextMenu menu;

        ConversationCell() {
            // largura 0 = a célula acompanha a lista (sem barra de rolagem horizontal)
            setPrefWidth(0);
            title.getStyleClass().add("conversation-title");
            title.setMinWidth(0);
            date.getStyleClass().add("conversation-date");
            VBox texts = new VBox(2, title, date);
            texts.setMinWidth(0);
            HBox.setHgrow(texts, Priority.ALWAYS);

            rename.getStyleClass().addAll("icon-button", "cell-action");
            rename.setTooltip(new Tooltip("Renomear"));
            delete.getStyleClass().addAll("icon-button", "cell-action", "danger-action");
            delete.setTooltip(new Tooltip("Excluir"));
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.visibleProperty().bind(hoverProperty().or(selectedProperty()));

            Region icon = Icons.create(Icons.CHAT, 14);
            root = new HBox(10, icon, texts, actions);
            root.setAlignment(Pos.CENTER_LEFT);

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
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setContextMenu(null);
                return;
            }
            title.setText(item.getTitle());
            date.setText(DateFormat.smart(item.getUpdatedAt()));
            setText(null);
            setGraphic(root);
            setContextMenu(menu);
        }
    }
}
