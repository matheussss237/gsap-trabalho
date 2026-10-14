package br.com.projeto.controller;

import br.com.projeto.Main;
import br.com.projeto.service.AuthenticationService;
import br.com.projeto.service.PasswordRecoveryService;
import br.com.projeto.util.Security;
import br.com.projeto.util.Ui;
import br.com.projeto.view.AuthView;
import javafx.scene.Parent;

/**
 * Ações das telas de login, cadastro e recuperação de senha.
 * As regras ficam nos Services; aqui só há validação visual dos campos e navegação.
 */
public class AuthController {

    private final Main app;
    private final AuthenticationService auth;
    private final PasswordRecoveryService recovery;
    private final ThemeController theme;

    public AuthController(Main app, AuthenticationService auth, PasswordRecoveryService recovery, ThemeController theme) {
        this.app = app;
        this.auth = auth;
        this.recovery = recovery;
        this.theme = theme;
    }

    // ---------- login ----------

    public Parent loginScreen(String email, String message) {
        AuthView view = create(AuthView.Mode.LOGIN);
        view.switchLink.setOnAction(e -> app.showRegister());
        view.forgotLink.setOnAction(e -> app.showForgotPassword(view.email.value()));
        view.submitButton.setOnAction(e -> login(view));
        if (email != null) view.email.input().setText(email);
        if (message != null) view.banner.success(message);
        return view.root();
    }

    private void login(AuthView view) {
        view.banner.hide();
        boolean ok = checkEmail(view);
        if (view.password.value().isEmpty()) {
            view.password.showError("Informe a sua senha.");
            ok = false;
        }
        if (!ok) return;
        view.submitButton.setLoading(true, "Entrando…");
        Ui.async(() -> auth.login(view.email.value(), view.password.value()), user -> app.showChat(), error -> {
            view.submitButton.setLoading(false, null);
            view.banner.error(error);
            view.password.input().clear();
            view.password.input().requestFocus();
        });
    }

    // ---------- cadastro ----------

    public Parent registerScreen() {
        AuthView view = create(AuthView.Mode.REGISTER);
        view.switchLink.setOnAction(e -> app.showLogin(null, null));
        view.submitButton.setOnAction(e -> register(view));
        view.password.input().textProperty().addListener((obs, old, value) -> updateStrength(view, value));
        return view.root();
    }

    private void register(AuthView view) {
        view.banner.hide();
        boolean ok = checkEmail(view);
        if (view.name.value().isBlank()) {
            view.name.showError("Informe o seu nome.");
            ok = false;
        }
        if (view.password.value().length() < Security.MIN_PASSWORD_LENGTH) {
            view.password.showError("A senha deve ter pelo menos " + Security.MIN_PASSWORD_LENGTH + " caracteres.");
            ok = false;
        }
        if (!view.password.value().equals(view.confirmation.value())) {
            view.confirmation.showError("As senhas não conferem.");
            ok = false;
        }
        if (!ok) return;
        view.submitButton.setLoading(true, "Criando conta…");
        Ui.async(() -> auth.register(view.name.value(), view.email.value(), view.password.value(),
                        view.confirmation.value()),
                user -> app.showLogin(user.getEmail(), "Conta criada com sucesso! Faça login para continuar."),
                error -> {
                    view.submitButton.setLoading(false, null);
                    view.banner.error(error);
                });
    }

    private void updateStrength(AuthView view, String password) {
        int score = 0;
        if (password.length() >= Security.MIN_PASSWORD_LENGTH) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*")) score++;
        if (password.matches(".*[A-Z].*") && password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[^A-Za-z0-9].*")) score++;
        String[] labels = {"Muito fraca", "Fraca", "Razoável", "Boa", "Forte", "Muito forte"};
        view.strengthBar.setProgress(password.isEmpty() ? 0 : Math.max(0.1, score / 5.0));
        view.strengthLabel.setText(password.isEmpty() ? " " : "Força da senha: " + labels[score]);
        view.strengthBar.getStyleClass().removeAll("weak", "medium", "strong");
        view.strengthBar.getStyleClass().add(score <= 1 ? "weak" : score <= 3 ? "medium" : "strong");
    }

    // ---------- recuperação de senha ----------

    public Parent forgotPasswordScreen(String email) {
        AuthView view = create(AuthView.Mode.FORGOT_PASSWORD);
        if (email != null) view.email.input().setText(email);
        view.switchLink.setOnAction(e -> app.showLogin(null, null));
        view.requestCodeButton.setOnAction(e -> requestCode(view));
        view.resendLink.setOnAction(e -> {
            view.showStepOne();
            requestCode(view);
        });
        view.submitButton.setOnAction(e -> resetPassword(view));
        return view.root();
    }

    private void requestCode(AuthView view) {
        view.banner.hide();
        if (!checkEmail(view)) return;
        view.requestCodeButton.setLoading(true, "Gerando código…");
        Ui.async(() -> recovery.requestReset(view.email.value()), result -> {
            view.requestCodeButton.setLoading(false, null);
            long minutes = PasswordRecoveryService.CODE_VALIDITY.toMinutes();
            if (result.sentByEmail()) {
                view.banner.success("Enviamos um código para o seu e-mail.");
                view.showStepTwo("Digite o código recebido. Ele vale por " + minutes + " minutos e só pode ser usado uma vez.");
            } else {
                view.banner.info("Modo demonstração (SMTP não configurado). Seu código é: " + result.demoCode().orElse(""));
                view.showStepTwo("Em produção o código seria enviado por e-mail. Ele vale por " + minutes
                        + " minutos e só pode ser usado uma vez.");
            }
        }, error -> {
            view.requestCodeButton.setLoading(false, null);
            view.banner.error(error);
        });
    }

    private void resetPassword(AuthView view) {
        view.banner.hide();
        if (view.code.value().isBlank()) {
            view.code.showError("Informe o código recebido.");
            return;
        }
        if (!view.password.value().equals(view.confirmation.value())) {
            view.confirmation.showError("As senhas não conferem.");
            return;
        }
        String email = view.email.value();
        view.submitButton.setLoading(true, "Redefinindo…");
        Ui.async(() -> {
            recovery.resetPassword(email, view.code.value().trim(), view.password.value(), view.confirmation.value());
            return true;
        }, ok -> app.showLogin(email, "Senha redefinida com sucesso! Entre com a nova senha."), error -> {
            view.submitButton.setLoading(false, null);
            view.banner.error(error);
        });
    }

    // ---------- auxiliares ----------

    private AuthView create(AuthView.Mode mode) {
        AuthView view = new AuthView(mode);
        theme.bindToggleButton(view.themeButton);
        return view;
    }

    private boolean checkEmail(AuthView view) {
        String email = view.email.value();
        if (email.isBlank()) {
            view.email.showError("Informe o seu e-mail.");
            return false;
        }
        if (!Security.isValidEmail(email)) {
            view.email.showError("Formato de e-mail inválido.");
            return false;
        }
        return true;
    }
}
