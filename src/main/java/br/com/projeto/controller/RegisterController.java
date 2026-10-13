package br.com.projeto.controller;

import br.com.projeto.service.AuthenticationService;
import br.com.projeto.util.FxAsync;
import br.com.projeto.util.Validator;
import br.com.projeto.view.Navigator;
import br.com.projeto.view.RegisterView;

/** Ações da tela de cadastro. */
public class RegisterController {

    private final RegisterView view;
    private final AuthenticationService authService;
    private final Navigator navigator;

    public RegisterController(RegisterView view, AuthenticationService authService, Navigator navigator) {
        this.view = view;
        this.authService = authService;
        this.navigator = navigator;

        view.registerButton().setOnAction(e -> register());
        view.loginLink().setOnAction(e -> navigator.showLogin(null, null));
        view.password().input().textProperty().addListener((obs, old, value) -> updateStrength(value));
    }

    private void register() {
        view.banner().hide();
        String name = view.name().value();
        String email = view.email().value();
        String password = view.password().value();
        String confirmation = view.confirmation().value();

        boolean valid = true;
        if (name == null || name.isBlank()) {
            view.name().showError("Informe o seu nome.");
            valid = false;
        }
        if (email == null || email.isBlank()) {
            view.email().showError("Informe o seu e-mail.");
            valid = false;
        } else if (!Validator.isValidEmail(email)) {
            view.email().showError("Formato de e-mail inválido.");
            valid = false;
        }
        if (password == null || password.length() < Validator.MIN_PASSWORD_LENGTH) {
            view.password().showError("A senha deve ter pelo menos " + Validator.MIN_PASSWORD_LENGTH + " caracteres.");
            valid = false;
        }
        if (password != null && !password.equals(confirmation)) {
            view.confirmation().showError("As senhas não conferem.");
            valid = false;
        }
        if (!valid) {
            return;
        }

        view.registerButton().setLoading(true, "Criando conta…");
        FxAsync.run(() -> authService.register(name, email, password, confirmation),
                user -> navigator.showLogin(user.getEmail(), "Conta criada com sucesso! Faça login para continuar."),
                error -> {
                    view.registerButton().setLoading(false);
                    view.banner().error(error);
                });
    }

    /** Indicador visual simples de força da senha. */
    private void updateStrength(String password) {
        int score = 0;
        if (password.length() >= Validator.MIN_PASSWORD_LENGTH) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*")) score++;
        if (password.matches(".*[A-Z].*") && password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[^A-Za-z0-9].*")) score++;

        String[] labels = {"Muito fraca", "Fraca", "Razoável", "Boa", "Forte", "Muito forte"};
        view.strengthBar().setProgress(password.isEmpty() ? 0 : Math.max(0.1, score / 5.0));
        view.strengthLabel().setText(password.isEmpty() ? " " : "Força da senha: " + labels[score]);
        view.strengthBar().getStyleClass().removeAll("weak", "medium", "strong");
        view.strengthBar().getStyleClass().add(score <= 1 ? "weak" : score <= 3 ? "medium" : "strong");
    }
}
