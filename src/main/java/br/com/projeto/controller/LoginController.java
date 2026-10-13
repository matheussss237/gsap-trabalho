package br.com.projeto.controller;

import br.com.projeto.service.AuthenticationService;
import br.com.projeto.util.FxAsync;
import br.com.projeto.util.Validator;
import br.com.projeto.view.LoginView;
import br.com.projeto.view.Navigator;

/** Ações da tela de login. A regra de autenticação fica no AuthenticationService. */
public class LoginController {

    private final LoginView view;
    private final AuthenticationService authService;
    private final Navigator navigator;

    public LoginController(LoginView view, AuthenticationService authService, Navigator navigator) {
        this.view = view;
        this.authService = authService;
        this.navigator = navigator;

        view.loginButton().setOnAction(e -> login());
        view.registerLink().setOnAction(e -> navigator.showRegister());
        view.forgotPasswordLink().setOnAction(e -> navigator.showForgotPassword(view.email().value()));
    }

    public void prefill(String email, String successMessage) {
        if (email != null) {
            view.email().input().setText(email);
        }
        if (successMessage != null) {
            view.banner().success(successMessage);
        }
    }

    private void login() {
        view.banner().hide();
        String email = view.email().value();
        String password = view.password().value();

        boolean valid = true;
        if (email == null || email.isBlank()) {
            view.email().showError("Informe o seu e-mail.");
            valid = false;
        } else if (!Validator.isValidEmail(email)) {
            view.email().showError("Formato de e-mail inválido.");
            valid = false;
        }
        if (password == null || password.isEmpty()) {
            view.password().showError("Informe a sua senha.");
            valid = false;
        }
        if (!valid) {
            return;
        }

        view.loginButton().setLoading(true, "Entrando…");
        FxAsync.run(() -> authService.login(email, password),
                user -> navigator.showChat(),
                error -> {
                    view.loginButton().setLoading(false);
                    view.banner().error(error);
                    view.password().input().clear();
                    view.password().input().requestFocus();
                });
    }
}
