package br.com.projeto.view;

import br.com.projeto.config.AppConfig;
import br.com.projeto.view.components.Icons;
import br.com.projeto.view.components.LogoMark;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Estrutura comum das telas de autenticação: painel da marca à esquerda e
 * cartão do formulário à direita. Abaixo de 860px o painel da marca é ocultado.
 */
public class AuthLayout extends HBox {

    private final VBox card = new VBox(18);
    private final Button themeButton = new Button();

    public AuthLayout(String heading, String subheading) {
        getStyleClass().add("auth-root");

        VBox brand = buildBrandPanel();
        brand.managedProperty().bind(brand.visibleProperty());
        widthProperty().addListener((obs, old, width) -> brand.setVisible(width.doubleValue() >= 860));

        Label title = new Label(heading);
        title.getStyleClass().add("auth-title");
        Label subtitle = new Label(subheading);
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);

        card.getStyleClass().add("auth-card");
        card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setFillWidth(true);
        card.getChildren().addAll(new VBox(6, title, subtitle));

        themeButton.getStyleClass().add("icon-button");
        themeButton.setTooltip(new Tooltip("Alternar tema claro/escuro"));
        HBox topBar = new HBox(themeButton);
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(16, 20, 0, 0));

        StackPane centered = new StackPane(card);
        centered.setPadding(new Insets(10, 32, 32, 32));
        StackPane.setAlignment(card, Pos.CENTER);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("transparent-scroll");

        BorderPane formSide = new BorderPane(scroll);
        formSide.setTop(topBar);
        formSide.getStyleClass().add("auth-form-side");
        HBox.setHgrow(formSide, Priority.ALWAYS);

        getChildren().addAll(brand, formSide);
    }

    private VBox buildBrandPanel() {
        Label name = new Label(AppConfig.APP_NAME);
        name.getStyleClass().add("brand-name");
        Label tagline = new Label("Um assistente de IA que conhece a sua empresa.");
        tagline.getStyleClass().add("brand-tagline");
        tagline.setWrapText(true);

        VBox features = new VBox(14,
                feature(Icons.CHAT, "Conversas organizadas", "Histórico salvo no MongoDB Atlas, só seu."),
                feature(Icons.BOOK, "RAG transparente", "Veja quais documentos embasaram cada resposta."),
                feature(Icons.SPARK, "Respostas rápidas", "Modelos Llama servidos pela Groq."));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label footer = new Label("v" + AppConfig.APP_VERSION + " · JavaFX · MongoDB · Groq");
        footer.getStyleClass().add("brand-footer");

        VBox brand = new VBox(18, LogoMark.create(64), name, tagline, features, spacer, footer);
        brand.getStyleClass().add("brand-panel");
        brand.setPrefWidth(400);
        brand.setMinWidth(360);
        VBox.setMargin(features, new Insets(22, 0, 0, 0));
        return brand;
    }

    private Node feature(String icon, String title, String description) {
        StackPane iconBox = new StackPane(Icons.create(icon, 16));
        iconBox.getStyleClass().add("feature-icon");
        Label t = new Label(title);
        t.getStyleClass().add("feature-title");
        Label d = new Label(description);
        d.getStyleClass().add("feature-description");
        d.setWrapText(true);
        HBox row = new HBox(14, iconBox, new VBox(2, t, d));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public VBox card() {
        return card;
    }

    public Button themeButton() {
        return themeButton;
    }

    /** Link do tipo "Não tem conta? Criar conta". */
    public static HBox linkRow(String text, Button link) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        link.getStyleClass().add("link-button");
        HBox row = new HBox(4, label, link);
        row.setAlignment(Pos.CENTER);
        return row;
    }
}
