package br.com.projeto.view;

import br.com.projeto.view.components.Icons;
import br.com.projeto.view.components.LoadingButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Exibida quando não é possível conectar ao MongoDB na inicialização. */
public class ConnectionErrorView {

    private final StackPane root = new StackPane();
    private final Label message = new Label();
    private final LoadingButton retryButton = new LoadingButton("Tentar novamente");
    private final Button exitButton = new Button("Sair");

    public ConnectionErrorView(String errorMessage) {
        root.getStyleClass().add("auth-root");

        StackPane icon = new StackPane(Icons.create(Icons.ALERT, 28));
        icon.getStyleClass().add("error-icon");
        Label title = new Label("Não foi possível iniciar");
        title.getStyleClass().add("auth-title");
        message.setText(errorMessage);
        message.setWrapText(true);
        message.getStyleClass().add("auth-subtitle");
        Label help = new Label("Verifique a variável MONGODB_URI, sua conexão com a internet e se o seu IP "
                + "está liberado em Network Access no MongoDB Atlas. Consulte o README.");
        help.setWrapText(true);
        help.getStyleClass().add("muted");

        retryButton.setMaxWidth(200);
        exitButton.getStyleClass().add("secondary-button");
        HBox actions = new HBox(10, retryButton, exitButton);

        VBox card = new VBox(16, icon, title, message, help, actions);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(520);
        card.setMaxHeight(VBox.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(card);
        root.setPadding(new Insets(32));
    }

    public void setMessage(String text) { message.setText(text); }
    public Parent root() { return root; }
    public LoadingButton retryButton() { return retryButton; }
    public Button exitButton() { return exitButton; }
}
