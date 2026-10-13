package br.com.projeto.controller;

import br.com.projeto.model.KnowledgeDocument;
import br.com.projeto.rag.RetrievalResult;
import br.com.projeto.rag.ScoredDocument;
import br.com.projeto.rag.StopWords;
import br.com.projeto.rag.TextNormalizer;
import br.com.projeto.rag.Tokenizer;
import br.com.projeto.service.KnowledgeBaseService;
import br.com.projeto.service.RAGService;
import br.com.projeto.service.SessionManager;
import br.com.projeto.util.FxAsync;
import br.com.projeto.view.KnowledgeBaseView;
import br.com.projeto.view.components.Dialogs;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Ações da base de conhecimento e do painel de teste do RAG. */
public class KnowledgeController {

    private final KnowledgeBaseView view;
    private final KnowledgeBaseService knowledgeService;
    private final RAGService ragService;
    private final SessionManager session;

    public KnowledgeController(KnowledgeBaseView view, KnowledgeBaseService knowledgeService,
                               RAGService ragService, SessionManager session) {
        this.view = view;
        this.knowledgeService = knowledgeService;
        this.ragService = ragService;
        this.session = session;

        view.newDocumentButton().setOnAction(e -> {
            view.documentList().getSelectionModel().clearSelection();
            view.showNewDocument();
        });
        view.documentList().getSelectionModel().selectedItemProperty().addListener((obs, old, doc) -> {
            if (doc != null) {
                boolean mine = Objects.equals(doc.getOwnerId(), session.requireUserId());
                view.showDocument(doc, mine);
                view.banner().hide();
            }
        });
        view.saveButton().setOnAction(e -> save());
        view.deleteButton().setOnAction(e -> delete());
        view.searchButton().setOnAction(e -> search());
        view.queryField().setOnAction(e -> search());

        view.showNewDocument();
        load(null);
    }

    private void load(String selectId) {
        FxAsync.run(knowledgeService::listVisible, docs -> {
            view.documentList().getItems().setAll(docs);
            if (selectId != null) {
                docs.stream().filter(d -> d.getId().equals(selectId)).findFirst()
                        .ifPresent(d -> view.documentList().getSelectionModel().select(d));
            }
        }, error -> view.banner().error(error));
    }

    private void save() {
        view.banner().hide();
        String title = view.title().value();
        String content = view.content().getText();
        boolean valid = true;
        if (title == null || title.isBlank()) {
            view.title().showError("Informe o título.");
            valid = false;
        }
        if (content == null || content.isBlank()) {
            view.showContentError("Informe o conteúdo do documento.");
            valid = false;
        }
        if (!valid) return;

        view.saveButton().setLoading(true, "Salvando…");
        FxAsync.run(() -> knowledgeService.add(title, content, view.keywords().value()), saved -> {
            view.saveButton().setLoading(false);
            load(saved.getId());
            view.banner().success("Documento salvo e indexado com " + saved.getKeywords().size() + " palavras-chave.");
        }, error -> {
            view.saveButton().setLoading(false);
            view.banner().error(error);
        });
    }

    private void delete() {
        KnowledgeDocument doc = view.documentList().getSelectionModel().getSelectedItem();
        if (doc == null) return;
        if (!Dialogs.confirm(view.root().getScene().getWindow(), "Excluir documento?",
                "O documento \"" + doc.getTitle() + "\" deixará de ser usado pelo RAG.", "Excluir")) {
            return;
        }
        FxAsync.run(() -> {
            knowledgeService.delete(doc.getId());
            return true;
        }, ok -> {
            view.showNewDocument();
            load(null);
            view.banner().success("Documento excluído.");
        }, error -> view.banner().error(error));
    }

    /** Executa o mesmo RAG do chat e mostra cada etapa. */
    private void search() {
        String query = view.queryField().getText();
        if (query == null || query.isBlank()) return;

        List<String> tokens = Tokenizer.tokenize(TextNormalizer.normalize(query));
        Set<String> removed = new LinkedHashSet<>();
        for (String token : tokens) {
            if (StopWords.isStopWord(token)) removed.add(token);
        }

        view.searchButton().setDisable(true);
        FxAsync.run(() -> ragService.retrieve(query), result -> {
            view.searchButton().setDisable(false);
            showResult(result, removed);
        }, error -> {
            view.searchButton().setDisable(false);
            view.results().getChildren().setAll(new Label(error));
        });
    }

    private void showResult(RetrievalResult result, Set<String> removed) {
        view.keywordChips().getChildren().clear();
        result.queryKeywords().forEach(k -> view.keywordChips().getChildren().add(KnowledgeBaseView.chip(k, "chip-keyword")));
        if (result.queryKeywords().isEmpty()) {
            view.keywordChips().getChildren().add(muted("Nenhuma palavra relevante."));
        }
        view.stopWordChips().getChildren().clear();
        removed.forEach(w -> view.stopWordChips().getChildren().add(KnowledgeBaseView.chip(w, "chip-stopword")));
        if (removed.isEmpty()) {
            view.stopWordChips().getChildren().add(muted("Nenhuma."));
        }

        view.results().getChildren().clear();
        if (!result.hasContext()) {
            view.results().getChildren().add(muted("Nenhum documento encontrado. A IA responderá apenas com o "
                    + "conhecimento geral do modelo."));
            return;
        }
        int position = 1;
        for (ScoredDocument scored : result.documents()) {
            Label title = new Label(position++ + ". " + scored.document().getTitle());
            title.getStyleClass().add("settings-strong");
            Label score = new Label("Pontuação: " + scored.score() + "   ·   termos encontrados: "
                    + String.join(", ", scored.matchedTerms()));
            score.getStyleClass().add("muted");
            String preview = scored.document().getContent();
            Label snippet = new Label(preview.length() > 220 ? preview.substring(0, 220) + "…" : preview);
            snippet.setWrapText(true);
            snippet.getStyleClass().add("result-snippet");
            VBox card = new VBox(4, title, score, snippet);
            card.getStyleClass().add("result-card");
            view.results().getChildren().add(card);
        }
    }

    private Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        label.setWrapText(true);
        return label;
    }
}
