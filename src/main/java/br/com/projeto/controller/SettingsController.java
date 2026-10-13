package br.com.projeto.controller;

import br.com.projeto.config.AppConfig;
import br.com.projeto.config.AppContext;
import br.com.projeto.service.ThemeService.Theme;
import br.com.projeto.util.FxAsync;
import br.com.projeto.view.SettingsView;

/** Configurações: tema, troca de senha e status das integrações. */
public class SettingsController {

    private final SettingsView view;
    private final AppContext context;

    public SettingsController(SettingsView view, AppContext context, ThemeController themeController) {
        this.view = view;
        this.context = context;

        (themeController.current() == Theme.DARK ? view.darkOption() : view.lightOption()).setSelected(true);
        view.themeGroup().selectedToggleProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                themeController.setTheme(selected == view.darkOption() ? Theme.DARK : Theme.LIGHT);
            }
        });

        context.session().currentUser().ifPresent(user -> {
            view.accountName().setText(user.getName());
            view.accountEmail().setText(user.getEmail());
        });
        view.changePasswordButton().setOnAction(e -> changePassword());

        view.addStatus("MongoDB Atlas", "Conectado", true);
        boolean groq = context.aiClient().isConfigured();
        view.addStatus("Groq API", groq ? "Chave configurada (GROQ_API_KEY)" : "GROQ_API_KEY não configurada", groq);
        view.addStatus("Modelo de IA", context.aiClient().modelName(), true);
        view.addStatus("E-mail (SMTP)", context.emailConfigured()
                ? "Configurado — códigos de recuperação enviados por e-mail"
                : "Não configurado — recuperação em modo demonstração", context.emailConfigured());
        view.addStatus("Arquivo opcional", AppConfig.configFile().toString(), true);
    }

    private void changePassword() {
        view.passwordBanner().hide();
        String current = view.currentPassword().value();
        String password = view.newPassword().value();
        String confirmation = view.confirmation().value();
        if (current == null || current.isEmpty()) {
            view.currentPassword().showError("Informe a senha atual.");
            return;
        }
        view.changePasswordButton().setLoading(true, "Alterando…");
        FxAsync.run(() -> {
            context.userService().changePassword(current, password, confirmation);
            return true;
        }, ok -> {
            view.changePasswordButton().setLoading(false);
            view.currentPassword().input().clear();
            view.newPassword().input().clear();
            view.confirmation().input().clear();
            view.passwordBanner().success("Senha alterada com sucesso.");
        }, error -> {
            view.changePasswordButton().setLoading(false);
            view.passwordBanner().error(error);
        });
    }
}
