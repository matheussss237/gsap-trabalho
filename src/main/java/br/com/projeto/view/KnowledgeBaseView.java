package br.com.projeto.view;

import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.view.components.FormField;
import br.com.projeto.view.components.Icons;
import br.com.projeto.view.components.LoadingButton;
import br.com.projeto.view.components.StatusBanner;
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
 * Base de conhecimento do RAG: cadastro/consulta de documentos e um painel
 * "Testar recuperação" que mostra palavras-chave, stop words removidas e pontuação.
 */
public class KnowledgeBaseView {

    private final BorderPane root = new BorderPane();

    // aba Documentos
    private final ListView<KnowledgeDocument> documentList = new ListView<>();
    private final Button newDocumentButton = new Button("Novo documento", Icons.create(Icons.PLUS, 12));
    private final Label editorTitle = new Label("Novo documento");
    private final FormField title = FormField.text("Título", "Ex.: Política de férias");
    private final FormField keywords = FormField.text("Palavras-chave (separadas por vírgula)", "férias, rh, benefícios");
    private final TextArea content = new TextArea();
    private final Label contentError = new Label();
    private final Label storedKeywords = new Label();
    private final LoadingButton saveButton = new LoadingButton("Salvar documento");
    private final Button deleteButton = new Button("Excluir", Icons.create(Icons.TRASH, 12));
    private final StatusBanner banner = new StatusBanner();

    // aba Testar recuperação
    private final TextField queryField = new TextField();
    private final Button searchButton = new Button("Pesquisar", Icons.create(Icons.SEARCH, 12));
    private final FlowPane keywordChips = new FlowPane(6, 6);
    private final FlowPane stopWordChips = new FlowPane(6, 6);
    private final VBox results = new VBox(10);

    private final Button closeButton = new Button("Fechar");

    public KnowledgeBaseView() {
        root.getStyleClass().add("knowledge-root");

        TabPane tabs = new TabPane(new Tab("Documentos", buildDocumentsTab()),
                new Tab("Testar recuperação (RAG)", buildSearchTab()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("knowledge-tabs");

        Label heading = new Label("Base de conhecimento");
        heading.getStyleClass().add("dialog-title");
        Label sub = new Label("Documentos usados pelo RAG para contextualizar as respostas da IA.");
        sub.getStyleClass().add("muted");
        VBox header = new VBox(2, heading, sub);
        header.getStyleClass().add("dialog-header");

        closeButton.getStyleClass().add("secondary-button");
        closeButton.setCancelButton(true);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("dialog-footer");

        root.setTop(header);
        root.setCenter(tabs);
        root.setBottom(footer);
    }

    private BorderPane buildDocumentsTab() {
        newDocumentButton.getStyleClass().add("new-chat-button");
        newDocumentButton.setMaxWidth(Double.MAX_VALUE);
        documentList.getStyleClass().add("document-list");
        documentList.setCellFactory(list -> new DocumentCell());
        VBox.setVgrow(documentList, Priority.ALWAYS);
        VBox left = new VBox(10, newDocumentButton, documentList);
        left.setPrefWidth(270);
        left.setMinWidth(220);
        left.getStyleClass().add("knowledge-list-pane");

        editorTitle.getStyleClass().add("section-heading");
        content.setWrapText(true);
        content.setPrefRowCount(10);
        content.setPromptText("Conteúdo do documento que poderá ser usado como contexto pela IA…");
        content.getStyleClass().add("form-input");
        content.textProperty().addListener((o, a, b) -> hideContentError());
        Label contentLabel = new Label("Conteúdo");
        contentLabel.getStyleClass().add("form-label");
        contentError.getStyleClass().add("field-error");
        hideContentError();
        VBox.setVgrow(content, Priority.ALWAYS);

        storedKeywords.getStyleClass().add("muted");
        storedKeywords.setWrapText(true);
        storedKeywords.managedProperty().bind(storedKeywords.visibleProperty());

        saveButton.setMaxWidth(220);
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.managedProperty().bind(deleteButton.visibleProperty());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, saveButton, spacer, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox editor = new VBox(12, editorTitle, banner, title, keywords,
                new VBox(6, contentLabel, content, contentError), storedKeywords, actions);
        editor.setPadding(new Insets(4, 4, 4, 18));
        VBox.setVgrow(editor.getChildren().get(4), Priority.ALWAYS);

        BorderPane pane = new BorderPane(editor);
        pane.setLeft(left);
        pane.setPadding(new Insets(16, 24, 8, 24));
        return pane;
    }

    private ScrollPane buildSearchTab() {
        Label explanation = new Label("Digite uma pergunta como faria no chat. O painel mostra cada etapa do RAG: "
                + "normalização, remoção de stop words, palavras-chave e os documentos encontrados com a pontuação.");
        explanation.setWrapText(true);
        explanation.getStyleClass().add("muted");

        queryField.setPromptText("Ex.: Qual é o horário do suporte técnico da empresa?");
        queryField.getStyleClass().add("form-input");
        HBox.setHgrow(queryField, Priority.ALWAYS);
        searchButton.getStyleClass().add("primary-button");
        searchButton.setDefaultButton(false);
        HBox searchRow = new HBox(10, queryField, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        Label kwTitle = new Label("Palavras-chave extraídas");
        kwTitle.getStyleClass().add("form-label");
        Label swTitle = new Label("Stop words removidas");
        swTitle.getStyleClass().add("form-label");
        Label resTitle = new Label("Documentos recuperados");
        resTitle.getStyleClass().add("form-label");

        VBox box = new VBox(14, explanation, searchRow, kwTitle, keywordChips, swTitle, stopWordChips, resTitle, results);
        box.setPadding(new Insets(20, 24, 20, 24));
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        return scroll;
    }

    public void showContentError(String message) {
        contentError.setText(message);
        contentError.setVisible(true);
        contentError.setManaged(true);
    }

    private void hideContentError() {
        contentError.setVisible(false);
        contentError.setManaged(false);
    }

    /** Prepara o editor para um documento existente (somente leitura se for público). */
    public void showDocument(KnowledgeDocument doc, boolean editable) {
        editorTitle.setText(doc.isPublic() ? "Documento público (exemplo)" : "Seu documento");
        title.input().setText(doc.getTitle());
        keywords.input().setText("");
        content.setText(doc.getContent());
        storedKeywords.setText("Palavras-chave indexadas: " + String.join(", ", doc.getKeywords()));
        storedKeywords.setVisible(true);
        setEditable(false);
        deleteButton.setVisible(editable);
        saveButton.setVisible(false);
        saveButton.setManaged(false);
        keywords.setVisible(false);
        keywords.setManaged(false);
    }

    public void showNewDocument() {
        editorTitle.setText("Novo documento");
        title.input().clear();
        keywords.input().clear();
        content.clear();
        storedKeywords.setVisible(false);
        setEditable(true);
        deleteButton.setVisible(false);
        saveButton.setVisible(true);
        saveButton.setManaged(true);
        keywords.setVisible(true);
        keywords.setManaged(true);
        banner.hide();
    }

    private void setEditable(boolean editable) {
        title.input().setEditable(editable);
        content.setEditable(editable);
    }

    public static Label chip(String text, String styleClass) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("keyword-chip", styleClass);
        return chip;
    }

    public Parent root() { return root; }
    public ListView<KnowledgeDocument> documentList() { return documentList; }
    public Button newDocumentButton() { return newDocumentButton; }
    public FormField title() { return title; }
    public FormField keywords() { return keywords; }
    public TextArea content() { return content; }
    public LoadingButton saveButton() { return saveButton; }
    public Button deleteButton() { return deleteButton; }
    public StatusBanner banner() { return banner; }
    public TextField queryField() { return queryField; }
    public Button searchButton() { return searchButton; }
    public FlowPane keywordChips() { return keywordChips; }
    public FlowPane stopWordChips() { return stopWordChips; }
    public VBox results() { return results; }
    public Button closeButton() { return closeButton; }

    private static class DocumentCell extends ListCell<KnowledgeDocument> {
        DocumentCell() {
            setPrefWidth(0);
        }

        @Override
        protected void updateItem(KnowledgeDocument item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label t = new Label(item.getTitle());
            t.getStyleClass().add("conversation-title");
            Label badge = new Label(item.isPublic() ? "público" : "meu");
            badge.getStyleClass().addAll("badge", item.isPublic() ? "badge-public" : "badge-mine");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(8, Icons.create(Icons.DOC, 13), t, spacer, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            t.setMinWidth(0);
            setText(null);
            setGraphic(row);
        }
    }
}
