package br.com.projeto.controller;

import br.com.projeto.model.Conversation;
import br.com.projeto.service.AuthenticationService;
import br.com.projeto.service.ConversationService;
import br.com.projeto.util.FxAsync;
import br.com.projeto.view.Navigator;
import br.com.projeto.view.SidebarView;
import br.com.projeto.view.components.Dialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.Locale;
import java.util.function.Consumer;

/** Ações da sidebar: listar, criar, renomear, excluir e buscar conversas; logout. */
public class SidebarController {

    private final SidebarView view;
    private final ConversationService conversationService;
    private final AuthenticationService authService;
    private final Navigator navigator;

    private final ObservableList<Conversation> conversations = FXCollections.observableArrayList();
    private final FilteredList<Conversation> filtered = new FilteredList<>(conversations, c -> true);

    private Consumer<Conversation> onSelect = c -> { };
    private Consumer<Conversation> onRenamed = c -> { };
    private Consumer<Conversation> onDeleted = c -> { };
    private Consumer<String> onError = message -> { };

    public SidebarController(SidebarView view, ConversationService conversationService,
                             AuthenticationService authService, Navigator navigator) {
        this.view = view;
        this.conversationService = conversationService;
        this.authService = authService;
        this.navigator = navigator;

        view.conversationList().setItems(filtered);
        view.conversationList().getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && (old == null || !selected.getId().equals(old.getId()))) {
                onSelect.accept(selected);
            }
        });
        view.searchField().textProperty().addListener((obs, old, text) -> {
            String term = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(c -> term.isEmpty() || c.getTitle().toLowerCase(Locale.ROOT).contains(term));
        });

        view.newConversationButton().setOnAction(e -> createConversation());
        view.setOnRename(this::rename);
        view.setOnDelete(this::delete);
        view.logoutButton().setOnAction(e -> logout());
        view.knowledgeButton().setOnAction(e -> navigator.openKnowledgeBase());
        view.settingsButton().setOnAction(e -> navigator.openSettings());
    }

    public void setOnSelect(Consumer<Conversation> handler) { this.onSelect = handler; }
    public void setOnRenamed(Consumer<Conversation> handler) { this.onRenamed = handler; }
    public void setOnDeleted(Consumer<Conversation> handler) { this.onDeleted = handler; }
    public void setOnError(Consumer<String> handler) { this.onError = handler; }

    /** Carrega apenas as conversas do usuário logado. */
    public void load() {
        FxAsync.run(conversationService::listMine, list -> {
            conversations.setAll(list);
            if (!list.isEmpty()) {
                view.conversationList().getSelectionModel().selectFirst();
            } else {
                onSelect.accept(null);
            }
        }, onError);
    }

    public void createConversation() {
        view.newConversationButton().setDisable(true);
        FxAsync.run(conversationService::create, created -> {
            view.newConversationButton().setDisable(false);
            view.searchField().clear();
            conversations.add(0, created);
            view.conversationList().getSelectionModel().select(created);
            view.conversationList().scrollTo(0);
        }, error -> {
            view.newConversationButton().setDisable(false);
            onError.accept(error);
        });
    }

    /** Insere ou atualiza a conversa e a move para o topo (mais recente). */
    public void upsert(Conversation conversation) {
        int index = indexOf(conversation.getId());
        Conversation selected = view.conversationList().getSelectionModel().getSelectedItem();
        boolean wasSelected = selected != null && selected.getId().equals(conversation.getId());
        if (index >= 0) {
            conversations.remove(index);
        }
        conversations.add(0, conversation);
        if (wasSelected || selected == null) {
            selectSilently(conversation);
        }
    }

    /** Seleciona sem disparar novo carregamento do chat. */
    public void selectSilently(Conversation conversation) {
        Consumer<Conversation> handler = onSelect;
        onSelect = c -> { };
        view.conversationList().getSelectionModel().select(conversation);
        onSelect = handler;
    }

    public void rename(Conversation conversation) {
        if (conversation == null) return;
        Dialogs.input(view.getScene().getWindow(), "Renomear conversa", "Novo título:", conversation.getTitle())
                .filter(title -> !title.isBlank() && !title.equals(conversation.getTitle()))
                .ifPresent(title -> FxAsync.run(() -> conversationService.rename(conversation.getId(), title),
                        updated -> {
                            int index = indexOf(updated.getId());
                            if (index >= 0) {
                                conversations.set(index, updated);
                                selectSilently(updated);
                            }
                            onRenamed.accept(updated);
                        },
                        error -> Dialogs.error(view.getScene().getWindow(), "Não foi possível renomear", error)));
    }

    public void delete(Conversation conversation) {
        if (conversation == null) return;
        boolean confirmed = Dialogs.confirm(view.getScene().getWindow(), "Excluir conversa?",
                "A conversa \"" + conversation.getTitle() + "\" e todas as suas mensagens serão excluídas "
                        + "definitivamente. Esta ação não pode ser desfeita.", "Excluir");
        if (!confirmed) return;

        FxAsync.run(() -> {
            conversationService.delete(conversation.getId());
            return conversation;
        }, deleted -> {
            int index = indexOf(deleted.getId());
            if (index >= 0) {
                conversations.remove(index);
            }
            onDeleted.accept(deleted);
            if (conversations.isEmpty()) {
                onSelect.accept(null);
            } else {
                view.conversationList().getSelectionModel().selectFirst();
            }
        }, error -> Dialogs.error(view.getScene().getWindow(), "Erro ao excluir conversa", error));
    }

    private void logout() {
        authService.logout();
        navigator.showLogin(null, "Você saiu da sua conta.");
    }

    private int indexOf(String id) {
        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
