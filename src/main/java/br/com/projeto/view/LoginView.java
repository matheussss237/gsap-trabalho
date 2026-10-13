package br.com.projeto.view;

import br.com.projeto.view.components.FormField;
import br.com.projeto.view.components.LoadingButton;
import br.com.projeto.view.components.StatusBanner;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/** Tela de login (construída por código, sem Scene Builder). */
public class LoginView {

    private final AuthLayout layout = new AuthLayout("Bem-vindo de volta", "Entre com seu e-mail e senha para continuar.");
    private final FormField email = FormField.text("E-mail", "nome@empresa.com");
    private final FormField password = FormField.password("Senha", "Sua senha");
    private final LoadingButton loginButton = new LoadingButton("Entrar");
    private final Button forgotPasswordLink = new Button("Esqueci minha senha");
    private final Button registerLink = new Button("Criar conta");
    private final StatusBanner banner = new StatusBanner();

    public LoginView() {
        forgotPasswordLink.getStyleClass().add("link-button");
        HBox forgotRow = new HBox(forgotPasswordLink);
        forgotRow.setAlignment(Pos.CENTER_RIGHT);

        loginButton.setDefaultButton(true);
        layout.card().getChildren().addAll(banner, email, password, forgotRow, loginButton,
                AuthLayout.linkRow("Não tem uma conta?", registerLink));
    }

    public Parent root() { return layout; }
    public FormField email() { return email; }
    public FormField password() { return password; }
    public LoadingButton loginButton() { return loginButton; }
    public Button forgotPasswordLink() { return forgotPasswordLink; }
    public Button registerLink() { return registerLink; }
    public StatusBanner banner() { return banner; }
    public Button themeButton() { return layout.themeButton(); }
}
