package br.com.projeto.view;

import br.com.projeto.config.AppConfig;
import br.com.projeto.config.AppContext;
import br.com.projeto.controller.ChatController;
import br.com.projeto.controller.ForgotPasswordController;
import br.com.projeto.controller.KnowledgeController;
import br.com.projeto.controller.LoginController;
import br.com.projeto.controller.RegisterController;
import br.com.projeto.controller.SettingsController;
import br.com.projeto.controller.SidebarController;
import br.com.projeto.controller.ThemeController;
import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Navegação entre telas. Cada tela é composta por uma View (interface) e um
 * Controller (ações), que recebem os Services do AppContext.
 */
public class Navigator {

    private final Stage stage;
    private final Scene scene;
    private final AppContext context;
    private final ThemeController themeController;

    public Navigator(Stage stage, Scene scene, AppContext context, ThemeController themeController) {
        this.stage = stage;
        this.scene = scene;
        this.context = context;
        this.themeController = themeController;
    }

    public void showLogin(String email, String message) {
        LoginView view = new LoginView();
        themeController.bindToggleButton(view.themeButton());
        new LoginController(view, context.authenticationService(), this).prefill(email, message);
        setRoot(view.root(), "Entrar");
        if (email != null) {
            view.password().input().requestFocus();
        } else {
            view.email().input().requestFocus();
        }
    }

    public void showRegister() {
        RegisterView view = new RegisterView();
        themeController.bindToggleButton(view.themeButton());
        new RegisterController(view, context.authenticationService(), this);
        setRoot(view.root(), "Criar conta");
        view.name().input().requestFocus();
    }

    public void showForgotPassword(String email) {
        ForgotPasswordView view = new ForgotPasswordView();
        themeController.bindToggleButton(view.themeButton());
        new ForgotPasswordController(view, context.passwordRecoveryService(), this).prefill(email);
        setRoot(view.root(), "Recuperar senha");
    }

    public void showChat() {
        ChatView view = new ChatView();
        themeController.bindToggleButton(view.sidebar().themeButton());
        SidebarController sidebarController = new SidebarController(view.sidebar(),
                context.conversationService(), context.authenticationService(), this);
        new ChatController(view, context.chatService(), context.conversationService(), context.session(),
                sidebarController);
        setRoot(view.root(), null);
        sidebarController.load();
        view.input().requestFocus();
    }

    public void openKnowledgeBase() {
        KnowledgeBaseView view = new KnowledgeBaseView();
        Stage dialog = modal(view.root(), "Base de conhecimento", 980, 640);
        new KnowledgeController(view, context.knowledgeBaseService(), context.ragService(), context.session());
        view.closeButton().setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    public void openSettings() {
        SettingsView view = new SettingsView();
        Stage dialog = modal(view.root(), "Configurações", 640, 680);
        new SettingsController(view, context, themeController);
        view.closeButton().setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    private Stage modal(Parent root, String title, double width, double height) {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(title + " — " + AppConfig.APP_NAME);
        dialog.getIcons().setAll(stage.getIcons());
        Scene dialogScene = new Scene(root, width, height);
        Runnable unregister = themeController.register(dialogScene);
        dialog.setOnHidden(e -> unregister.run());
        dialog.setScene(dialogScene);
        dialog.setMinWidth(560);
        dialog.setMinHeight(480);
        return dialog;
    }

    /** Troca o conteúdo da janela com um fade suave. */
    private void setRoot(Parent root, String subtitle) {
        stage.setTitle(subtitle == null ? AppConfig.APP_NAME : AppConfig.APP_NAME + " — " + subtitle);
        root.setOpacity(0);
        scene.setRoot(root);
        FadeTransition fade = new FadeTransition(Duration.millis(260), root);
        fade.setToValue(1);
        fade.play();
    }
}
