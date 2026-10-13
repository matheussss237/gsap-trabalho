package br.com.projeto.view;

import br.com.projeto.util.Validator;
import br.com.projeto.view.components.FormField;
import br.com.projeto.view.components.LoadingButton;
import br.com.projeto.view.components.StatusBanner;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/** Tela de cadastro de usuário. */
public class RegisterView {

    private final AuthLayout layout = new AuthLayout("Criar conta", "Cadastre-se para começar a conversar com o Nexus.");
    private final FormField name = FormField.text("Nome", "Seu nome");
    private final FormField email = FormField.text("E-mail", "nome@empresa.com");
    private final FormField password = FormField.password("Senha",
            "Mínimo de " + Validator.MIN_PASSWORD_LENGTH + " caracteres, com letras e números");
    private final FormField confirmation = FormField.password("Confirmar senha", "Repita a senha");
    private final ProgressBar strengthBar = new ProgressBar(0);
    private final Label strengthLabel = new Label(" ");
    private final LoadingButton registerButton = new LoadingButton("Criar conta");
    private final Button loginLink = new Button("Entrar");
    private final StatusBanner banner = new StatusBanner();

    public RegisterView() {
        strengthBar.getStyleClass().add("strength-bar");
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        strengthLabel.getStyleClass().add("strength-label");
        VBox strength = new VBox(4, strengthBar, strengthLabel);

        registerButton.setDefaultButton(true);
        layout.card().getChildren().addAll(banner, name, email, password, strength, confirmation, registerButton,
                AuthLayout.linkRow("Já possui conta?", loginLink));
    }

    public Parent root() { return layout; }
    public FormField name() { return name; }
    public FormField email() { return email; }
    public FormField password() { return password; }
    public FormField confirmation() { return confirmation; }
    public ProgressBar strengthBar() { return strengthBar; }
    public Label strengthLabel() { return strengthLabel; }
    public LoadingButton registerButton() { return registerButton; }
    public Button loginLink() { return loginLink; }
    public StatusBanner banner() { return banner; }
    public Button themeButton() { return layout.themeButton(); }
}
