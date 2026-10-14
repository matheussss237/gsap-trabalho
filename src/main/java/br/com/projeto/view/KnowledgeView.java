package br.com.projeto.view;

import br.com.projeto.model.KnowledgeDocument;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Janela da base de conhecimento do RAG: cadastro/consulta de documentos e a aba
 * "Testar recuperação", que mostra palavras-chave, stop words removidas e pontuação.
 */
public class KnowledgeView {

    // aba Documentos
    public final ListView<KnowledgeDocument> documentList = new ListView<>();
    public final Button newDocumentButton = new Button("Novo documento", Components.icon(Components.PLUS, 12));
    public final Components.Field title = new Components.Field("Título", "Ex.: Política de férias", false);
    public final Components.Field keywords =
            new Components.Field("Palavras-chave (separadas por vírgula)", "férias, rh, benefícios", false);
    public final TextArea content = new TextArea();
    public final Components.LoadingButton saveButton = new Components.LoadingButton("Salvar documento");
    public final Button deleteButton = new Button("Excluir", Components.icon(Components.TRASH, 12));
    public final Components.Banner banner = new Components.Banner();

    // aba Testar recuperação
    public final TextField queryField = new TextField();
    public final Button searchButton = new Button("Pesquisar", Components.icon(Components.SEARCH, 12));
    public final FlowPane keywordChips = new FlowPane(6, 6);
    public final FlowPane stopWordChips = new FlowPane(6, 6);
    public final VBox results = new VBox(10);

    public final Button closeButton = new Button("Fechar");

    private final Label editorTitle = Components.label("Novo documento", "section-heading");
    private final Label storedKeywords = Components.label("", "muted");
    private final BorderPane root = new BorderPane();

    public KnowledgeView() {
        root.getStyleClass().add("knowledge-root");
        TabPane tabs = new TabPane(new Tab("Documentos", documentsTab()), new Tab("Testar recuperação (RAG)", searchTab()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox header = new VBox(2, Components.label("Base de conhecimento", "dialog-title"),
                Components.label("Documentos usados pelo RAG para contextualizar as respostas da Lumi.", "muted"));
        header.getStyleClass().add("dialog-header");
        closeButton.getStyleClass().add("secondary-button");
        closeButton.setCancelButton(true);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("dialog-footer");

        root.setTop(header);
        root.setCenter(tabs);
        root.setBottom(footer);
        showNewDocument();
    }

    private BorderPane documentsTab() {
        newDocumentButton.getStyleClass().add("new-chat-button");
        newDocumentButton.setMaxWidth(Double.MAX_VALUE);
        documentList.setCellFactory(list -> new ListCell<>() {
            { setPrefWidth(0); }

            @Override
            protected void updateItem(KnowledgeDocument item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label t = Components.label(item.getTitle(), "conversation-title");
                t.setMinWidth(0);
                Label badge = Components.label(item.isPublic() ? "público" : "meu", "badge");
                badge.getStyleClass().add(item.isPublic() ? "badge-public" : "badge-mine");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox row = new HBox(8, Components.icon(Components.DOC, 13), t, spacer, badge);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
            }
        });
        VBox.setVgrow(documentList, Priority.ALWAYS);
        VBox left = new VBox(10, newDocumentButton, documentList);
        left.setPrefWidth(270);
        left.setMinWidth(220);
        left.getStyleClass().add("knowledge-list-pane");

        content.setWrapText(true);
        content.setPromptText("Conteúdo do documento que poderá ser usado como contexto pela IA…");
        content.getStyleClass().add("form-input");
        VBox.setVgrow(content, Priority.ALWAYS);
        VBox contentBox = new VBox(6, Components.label("Conteúdo", "form-label"), content);
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        storedKeywords.setWrapText(true);
        storedKeywords.managedProperty().bind(storedKeywords.visibleProperty());
        keywords.managedProperty().bind(keywords.visibleProperty());
        saveButton.setMaxWidth(220);
        saveButton.managedProperty().bind(saveButton.visibleProperty());
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.managedProperty().bind(deleteButton.visibleProperty());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, saveButton, spacer, deleteButton);

        VBox editor = new VBox(12, editorTitle, banner, title, keywords, contentBox, storedKeywords, actions);
        editor.setPadding(new Insets(4, 4, 4, 18));
        BorderPane pane = new BorderPane(editor);
        pane.setLeft(left);
        pane.setPadding(new Insets(16, 24, 8, 24));
        return pane;
    }

    private ScrollPane searchTab() {
        Label explanation = Components.label("Digite uma pergunta como faria no chat. O painel mostra cada etapa do RAG: "
                + "remoção de stop words, palavras-chave e documentos encontrados com a pontuação.", "muted");
        explanation.setWrapText(true);
        queryField.setPromptText("Ex.: Qual é o horário do suporte técnico da empresa?");
        HBox.setHgrow(queryField, Priority.ALWAYS);
        searchButton.getStyleClass().add("primary-button");
        HBox searchRow = new HBox(10, queryField, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(14, explanation, searchRow,
                Components.label("Palavras-chave extraídas", "form-label"), keywordChips,
                Components.label("Stop words removidas", "form-label"), stopWordChips,
                Components.label("Documentos recuperados", "form-label"), results);
        box.setPadding(new Insets(20, 24, 20, 24));
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        return scroll;
    }

    /** Mostra um documento existente (somente leitura; "Excluir" só se for do usuário). */
    public void showDocument(KnowledgeDocument doc, boolean mine) {
        editorTitle.setText(doc.isPublic() ? "Documento público (exemplo)" : "Seu documento");
        title.input().setText(doc.getTitle());
        content.setText(doc.getContent());
        storedKeywords.setText("Palavras-chave indexadas: " + String.join(", ", doc.getKeywords()));
        setEditing(false);
        deleteButton.setVisible(mine);
        banner.hide();
    }

    public void showNewDocument() {
        editorTitle.setText("Novo documento");
        title.input().clear();
        keywords.input().clear();
        content.clear();
        setEditing(true);
        deleteButton.setVisible(false);
    }

    private void setEditing(boolean editing) {
        title.input().setEditable(editing);
        content.setEditable(editing);
        keywords.setVisible(editing);
        saveButton.setVisible(editing);
        storedKeywords.setVisible(!editing);
    }

    public static Label chip(String text, String styleClass) {
        Label chip = Components.label(text, "keyword-chip");
        chip.getStyleClass().add(styleClass);
        return chip;
    }

    public Parent root() {
        return root;
    }
}
