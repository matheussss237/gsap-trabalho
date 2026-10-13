package br.com.projeto.view;

import br.com.projeto.view.components.FormField;
import br.com.projeto.view.components.LoadingButton;
import br.com.projeto.view.components.StatusBanner;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Recuperação de senha em duas etapas:
 * 1) informar o e-mail e receber um código temporário;
 * 2) informar o código e cadastrar a nova senha.
 */
public class ForgotPasswordView {

    private final AuthLayout layout = new AuthLayout("Recuperar senha",
            "Informe seu e-mail. Enviaremos um código temporário para você definir uma nova senha.");

    private final FormField email = FormField.text("E-mail", "nome@empresa.com");
    private final LoadingButton requestButton = new LoadingButton("Enviar código");

    private final Label stepTwoInfo = new Label();
    private final FormField code = FormField.text("Código de verificação", "6 dígitos");
    private final FormField newPassword = FormField.password("Nova senha", "Mínimo de 8 caracteres, com letras e números");
    private final FormField confirmation = FormField.password("Confirmar nova senha", "Repita a nova senha");
    private final LoadingButton resetButton = new LoadingButton("Redefinir senha");
    private final Button resendLink = new Button("Reenviar código");
    private final VBox stepTwo;

    private final Button backLink = new Button("Voltar ao login");
    private final StatusBanner banner = new StatusBanner();

    public ForgotPasswordView() {
        stepTwoInfo.getStyleClass().add("muted");
        stepTwoInfo.setWrapText(true);
        resendLink.getStyleClass().add("link-button");
        stepTwo = new VBox(14, stepTwoInfo, code, newPassword, confirmation, resetButton, resendLink);
        stepTwo.managedProperty().bind(stepTwo.visibleProperty());
        stepTwo.setVisible(false);

        requestButton.setDefaultButton(true);
        layout.card().getChildren().addAll(banner, email, requestButton, stepTwo,
                AuthLayout.linkRow("Lembrou a senha?", backLink));
    }

    /** Alterna para a etapa 2 (código + nova senha). */
    public void showStepTwo(String info) {
        stepTwoInfo.setText(info);
        email.input().setDisable(true);
        requestButton.setVisible(false);
        requestButton.setManaged(false);
        requestButton.setDefaultButton(false);
        resetButton.setDefaultButton(true);
        stepTwo.setVisible(true);
        code.input().requestFocus();
    }

    public void showStepOne() {
        email.input().setDisable(false);
        requestButton.setVisible(true);
        requestButton.setManaged(true);
        requestButton.setDefaultButton(true);
        resetButton.setDefaultButton(false);
        stepTwo.setVisible(false);
    }

    public Parent root() { return layout; }
    public FormField email() { return email; }
    public LoadingButton requestButton() { return requestButton; }
    public FormField code() { return code; }
    public FormField newPassword() { return newPassword; }
    public FormField confirmation() { return confirmation; }
    public LoadingButton resetButton() { return resetButton; }
    public Button resendLink() { return resendLink; }
    public Button backLink() { return backLink; }
    public StatusBanner banner() { return banner; }
    public Button themeButton() { return layout.themeButton(); }
}
