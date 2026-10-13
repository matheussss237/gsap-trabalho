package br.com.projeto.controller;

import br.com.projeto.service.PasswordRecoveryService;
import br.com.projeto.util.FxAsync;
import br.com.projeto.util.Validator;
import br.com.projeto.view.ForgotPasswordView;
import br.com.projeto.view.Navigator;

/** Fluxo "Esqueci minha senha": solicitar código → validar código e definir nova senha. */
public class ForgotPasswordController {

    private final ForgotPasswordView view;
    private final PasswordRecoveryService recoveryService;
    private final Navigator navigator;

    public ForgotPasswordController(ForgotPasswordView view, PasswordRecoveryService recoveryService,
                                    Navigator navigator) {
        this.view = view;
        this.recoveryService = recoveryService;
        this.navigator = navigator;

        view.requestButton().setOnAction(e -> requestCode());
        view.resendLink().setOnAction(e -> {
            view.showStepOne();
            requestCode();
        });
        view.resetButton().setOnAction(e -> resetPassword());
        view.backLink().setOnAction(e -> navigator.showLogin(null, null));
    }

    public void prefill(String email) {
        if (email != null && !email.isBlank()) {
            view.email().input().setText(email);
        }
    }

    private void requestCode() {
        view.banner().hide();
        String email = view.email().value();
        if (!Validator.isValidEmail(email)) {
            view.email().showError("Informe um e-mail válido.");
            return;
        }
        view.requestButton().setLoading(true, "Gerando código…");
        FxAsync.run(() -> recoveryService.requestReset(email),
                result -> {
                    view.requestButton().setLoading(false);
                    long minutes = PasswordRecoveryService.TOKEN_VALIDITY.toMinutes();
                    if (result.sentByEmail()) {
                        view.banner().success("Enviamos um código para o seu e-mail.");
                        view.showStepTwo("Digite o código recebido por e-mail. Ele vale por " + minutes
                                + " minutos e só pode ser usado uma vez.");
                    } else {
                        view.banner().info("Modo demonstração (SMTP não configurado). Seu código é: "
                                + result.demoCode().orElse(""));
                        view.showStepTwo("Em produção o código seria enviado por e-mail. Ele vale por " + minutes
                                + " minutos e só pode ser usado uma vez.");
                    }
                },
                error -> {
                    view.requestButton().setLoading(false);
                    view.banner().error(error);
                });
    }

    private void resetPassword() {
        view.banner().hide();
        String email = view.email().value();
        String code = view.code().value();
        String password = view.newPassword().value();
        String confirmation = view.confirmation().value();

        if (code == null || code.isBlank()) {
            view.code().showError("Informe o código recebido.");
            return;
        }
        if (password != null && !password.equals(confirmation)) {
            view.confirmation().showError("As senhas não conferem.");
            return;
        }
        view.resetButton().setLoading(true, "Redefinindo…");
        FxAsync.run(() -> {
                    recoveryService.resetPassword(email, code.trim(), password, confirmation);
                    return true;
                },
                ok -> navigator.showLogin(email, "Senha redefinida com sucesso! Entre com a nova senha."),
                error -> {
                    view.resetButton().setLoading(false);
                    view.banner().error(error);
                });
    }
}
