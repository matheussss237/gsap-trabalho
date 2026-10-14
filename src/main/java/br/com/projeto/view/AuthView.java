package br.com.projeto.view;

import br.com.projeto.config.AppConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Telas de autenticação (login, cadastro e recuperação de senha), construídas por código.
 * Todas têm o painel da marca à esquerda (oculto abaixo de 860 px) e o formulário à direita.
 */
public class AuthView {

    public enum Mode { LOGIN, REGISTER, FORGOT_PASSWORD }

    public final Mode mode;
    public final Components.Field name = new Components.Field("Nome", "Seu nome", false);
    public final Components.Field email = new Components.Field("E-mail", "nome@empresa.com", false);
    public final Components.Field password;
    public final Components.Field confirmation;
    public final Components.Field code = new Components.Field("Código de verificação", "6 dígitos", false);
    public final ProgressBar strengthBar = new ProgressBar(0);
    public final Label strengthLabel = Components.label(" ", "strength-label");
    public final Components.LoadingButton submitButton;
    public final Components.LoadingButton requestCodeButton = new Components.LoadingButton("Enviar código");
    public final Button forgotLink = link("Esqueci minha senha");
    public final Button resendLink = link("Reenviar código");
    public final Button switchLink;
    public final Button themeButton = Components.iconButton(Components.SUN, "Alternar tema claro/escuro");
    public final Components.Banner banner = new Components.Banner();

    private final Label stepTwoInfo = Components.label("", "muted");
    private final VBox stepTwo = new VBox(14);
    private final HBox root = new HBox();

    public AuthView(Mode mode) {
        this.mode = mode;
        boolean forgot = mode == Mode.FORGOT_PASSWORD;
        password = new Components.Field(forgot ? "Nova senha" : "Senha",
                mode == Mode.LOGIN ? "Sua senha" : "Mínimo de 8 caracteres, com letras e números", true);
        confirmation = new Components.Field(forgot ? "Confirmar nova senha" : "Confirmar senha", "Repita a senha", true);
        submitButton = new Components.LoadingButton(switch (mode) {
            case LOGIN -> "Entrar";
            case REGISTER -> "Criar conta";
            case FORGOT_PASSWORD -> "Redefinir senha";
        });
        switchLink = link(mode == Mode.LOGIN ? "Criar conta" : mode == Mode.REGISTER ? "Entrar" : "Voltar ao login");

        VBox card = new VBox(18);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        switch (mode) {
            case LOGIN -> {
                HBox forgotRow = new HBox(forgotLink);
                forgotRow.setAlignment(Pos.CENTER_RIGHT);
                submitButton.setDefaultButton(true);
                card.getChildren().addAll(heading("Bem-vindo de volta", "Entre com seu e-mail e senha para continuar."),
                        banner, email, password, forgotRow, submitButton, linkRow("Não tem uma conta?"));
            }
            case REGISTER -> {
                strengthBar.getStyleClass().add("strength-bar");
                strengthBar.setMaxWidth(Double.MAX_VALUE);
                submitButton.setDefaultButton(true);
                card.getChildren().addAll(heading("Criar conta", "Cadastre-se para começar a conversar com a Lumi."),
                        banner, name, email, password, new VBox(4, strengthBar, strengthLabel), confirmation,
                        submitButton, linkRow("Já possui conta?"));
            }
            case FORGOT_PASSWORD -> {
                stepTwoInfo.setWrapText(true);
                stepTwo.getChildren().addAll(stepTwoInfo, code, password, confirmation, submitButton, resendLink);
                stepTwo.managedProperty().bind(stepTwo.visibleProperty());
                stepTwo.setVisible(false);
                requestCodeButton.managedProperty().bind(requestCodeButton.visibleProperty());
                requestCodeButton.setDefaultButton(true);
                card.getChildren().addAll(heading("Recuperar senha",
                                "Informe seu e-mail. Você receberá um código temporário para definir uma nova senha."),
                        banner, email, requestCodeButton, stepTwo, linkRow("Lembrou a senha?"));
            }
        }

        StackPane centered = new StackPane(card);
        centered.setPadding(new Insets(10, 32, 32, 32));
        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("transparent-scroll");

        HBox topBar = new HBox(themeButton);
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(16, 20, 0, 0));
        BorderPane formSide = new BorderPane(scroll);
        formSide.setTop(topBar);
        HBox.setHgrow(formSide, Priority.ALWAYS);

        VBox brand = brandPanel();
        brand.managedProperty().bind(brand.visibleProperty());
        root.widthProperty().addListener((obs, old, width) -> brand.setVisible(width.doubleValue() >= 860));
        root.getStyleClass().add("auth-root");
        root.getChildren().addAll(brand, formSide);
    }

    /** Recuperação de senha: passa para a etapa 2 (código + nova senha). */
    public void showStepTwo(String info) {
        stepTwoInfo.setText(info);
        email.input().setDisable(true);
        requestCodeButton.setVisible(false);
        requestCodeButton.setDefaultButton(false);
        submitButton.setDefaultButton(true);
        stepTwo.setVisible(true);
        code.input().requestFocus();
    }

    public void showStepOne() {
        email.input().setDisable(false);
        requestCodeButton.setVisible(true);
        requestCodeButton.setDefaultButton(true);
        submitButton.setDefaultButton(false);
        stepTwo.setVisible(false);
    }

    public Parent root() {
        return root;
    }

    private static Node heading(String title, String subtitle) {
        Label sub = Components.label(subtitle, "auth-subtitle");
        sub.setWrapText(true);
        return new VBox(6, Components.label(title, "auth-title"), sub);
    }

    private HBox linkRow(String text) {
        HBox row = new HBox(4, Components.label(text, "muted"), switchLink);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private static Button link(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("link-button");
        return button;
    }

    private static VBox brandPanel() {
        Label tagline = Components.label("Sua assistente de IA que conhece a empresa.", "brand-tagline");
        tagline.setWrapText(true);
        VBox features = new VBox(14,
                feature(Components.CHAT, "Conversas organizadas", "Histórico salvo no MongoDB Atlas, só seu."),
                feature(Components.BOOK, "RAG transparente", "Veja quais documentos embasaram cada resposta."),
                feature(Components.SPARK, "Feita para você", "A Lumi te chama pelo nome e lembra da conversa."));
        VBox.setMargin(features, new Insets(22, 0, 0, 0));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox brand = new VBox(18, Components.logo(64), Components.label(AppConfig.APP_NAME, "brand-name"), tagline,
                features, spacer, Components.label("v" + AppConfig.APP_VERSION + " · JavaFX · MongoDB · Groq",
                "brand-footer"));
        brand.getStyleClass().add("brand-panel");
        brand.setPrefWidth(400);
        brand.setMinWidth(360);
        return brand;
    }

    private static Node feature(String icon, String title, String description) {
        StackPane iconBox = new StackPane(Components.icon(icon, 16));
        iconBox.getStyleClass().add("feature-icon");
        Label d = Components.label(description, "feature-description");
        d.setWrapText(true);
        HBox row = new HBox(14, iconBox, new VBox(2, Components.label(title, "feature-title"), d));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
