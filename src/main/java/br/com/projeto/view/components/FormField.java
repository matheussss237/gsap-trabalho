package br.com.projeto.view.components;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;

/** Campo de formulário: rótulo + entrada + mensagem de erro do próprio campo. */
public class FormField extends VBox {

    private final TextInputControl input;
    private final Label error = new Label();

    private FormField(String label, TextInputControl input, String prompt) {
        this.input = input;
        input.setPromptText(prompt);
        input.getStyleClass().add("form-input");
        input.textProperty().addListener((obs, old, value) -> clearError());

        Label title = new Label(label);
        title.getStyleClass().add("form-label");
        title.setLabelFor(input);

        error.getStyleClass().add("field-error");
        error.setManaged(false);
        error.setVisible(false);
        error.setWrapText(true);

        getStyleClass().add("form-field");
        setSpacing(6);
        getChildren().addAll(title, input, error);
    }

    public static FormField text(String label, String prompt) {
        return new FormField(label, new TextField(), prompt);
    }

    public static FormField password(String label, String prompt) {
        return new FormField(label, new PasswordField(), prompt);
    }

    public TextInputControl input() {
        return input;
    }

    public String value() {
        return input.getText();
    }

    public void showError(String message) {
        error.setText(message);
        error.setManaged(true);
        error.setVisible(true);
        if (!input.getStyleClass().contains("invalid")) {
            input.getStyleClass().add("invalid");
        }
    }

    public void clearError() {
        error.setManaged(false);
        error.setVisible(false);
        input.getStyleClass().remove("invalid");
    }
}
