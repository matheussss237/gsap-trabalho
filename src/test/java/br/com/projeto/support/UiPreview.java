package br.com.projeto.support;

import br.com.projeto.controller.KnowledgeController;
import br.com.projeto.controller.SettingsController;
import br.com.projeto.controller.ThemeController;
import br.com.projeto.model.Conversation;
import br.com.projeto.service.ThemeService;
import br.com.projeto.view.KnowledgeBaseView;
import br.com.projeto.view.Navigator;
import br.com.projeto.view.SettingsView;
import br.com.projeto.view.SplashBridge;
import br.com.projeto.view.SplashView;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Ferramenta de desenvolvimento (não faz parte do app): abre as telas com repositórios
 * em memória e salva capturas em target/preview. Uso: ver scripts/preview-ui.sh
 */
public class UiPreview extends Application {

    private final TestContext ctx = new TestContext();
    private final Deque<Runnable> steps = new ArrayDeque<>();
    private ThemeController theme;
    private Stage stage;
    private Scene scene;
    private Navigator navigator;
    private SplashView splash;

    @Override
    public void start(Stage primary) {
        new File("target/preview").mkdirs();
        stage = primary;
        theme = new ThemeController(new ThemeService());
        theme.setTheme(ThemeService.Theme.DARK);
        ctx.app.knowledgeBaseService().seedIfEmpty();
        ctx.ai.answer = """
                ## Encapsulamento em Java

                Encapsulamento é o princípio que **esconde o estado interno** de um objeto.

                - atributos `private`
                - acesso por métodos públicos
                - validação centralizada

                ```java
                public class Conta {
                    private double saldo;
                    public void depositar(double valor) { saldo += valor; }
                }
                ```
                """;

        splash = new SplashView();
        splash.load(new SplashBridge(() -> {
            shot(splash.stage().getScene(), "01-splash-final");
            splash.setStatus("Tudo pronto!", false);
            splash.call("playExit()");
        }, this::afterSplash), () -> System.err.println("SPLASH LOAD FAILED"));
        splash.stage().show();
        splash.setStatus("Conectando ao MongoDB Atlas…", false);
        later(1.2, () -> shot(splash.stage().getScene(), "00-splash-meio"));
    }

    private void afterSplash() {
        splash.stage().close();
        scene = new Scene(new StackPane(), 1200, 760);
        theme.register(scene);
        stage.setScene(scene);
        stage.show();
        navigator = new Navigator(stage, scene, ctx.app, theme);

        steps.add(() -> navigator.showLogin(null, null));
        steps.add(() -> shot(scene, "02-login-escuro"));
        steps.add(() -> theme.setTheme(ThemeService.Theme.LIGHT));
        steps.add(() -> shot(scene, "03-login-claro"));
        steps.add(() -> navigator.showRegister());
        steps.add(() -> shot(scene, "04-cadastro"));
        steps.add(() -> navigator.showForgotPassword("usuario1@teste.com"));
        steps.add(() -> shot(scene, "05-recuperar-senha"));
        steps.add(() -> {
            ctx.app.authenticationService().register("Usuário Um", "usuario1@teste.com", "senha1234", "senha1234");
            ctx.app.authenticationService().login("usuario1@teste.com", "senha1234");
            theme.setTheme(ThemeService.Theme.DARK);
            navigator.showChat();
        });
        steps.add(() -> shot(scene, "06-chat-vazio"));
        steps.add(() -> {
            Conversation c = ctx.app.conversationService().create();
            ctx.app.chatService().send(c.getId(), "Explique o encapsulamento em Java");
            ctx.app.conversationService().rename(ctx.app.conversationService().create().getId(), "MongoDB Atlas");
            navigator.showChat();
        });
        steps.add(() -> ((javafx.scene.control.ListView<?>) scene.lookup(".conversation-list"))
                .getSelectionModel().select(1));
        steps.add(() -> shot(scene, "07-chat-escuro"));
        steps.add(() -> theme.setTheme(ThemeService.Theme.LIGHT));
        steps.add(() -> shot(scene, "08-chat-claro"));
        steps.add(() -> { stage.setWidth(780); });
        steps.add(() -> shot(scene, "09-chat-estreito"));
        steps.add(() -> {
            stage.setWidth(1200);
            KnowledgeBaseView view = new KnowledgeBaseView();
            Stage s = new Stage();
            Scene sc = new Scene(view.root(), 980, 640);
            theme.register(sc);
            s.setScene(sc);
            new KnowledgeController(view, ctx.app.knowledgeBaseService(), ctx.app.ragService(), ctx.app.session());
            s.show();
            view.queryField().setText("Qual é o horário do suporte técnico da empresa?");
            view.searchButton().fire();
            later(0.8, () -> shot(sc, "10-base-conhecimento"));
            later(1.0, () -> {
                ((javafx.scene.control.TabPane) view.root().lookup(".tab-pane")).getSelectionModel().select(1);
            });
            later(1.6, () -> {
                shot(sc, "11-teste-rag");
                s.close();
            });
        });
        steps.add(() -> {});
        steps.add(() -> {});
        steps.add(() -> {
            SettingsView view = new SettingsView();
            Stage s = new Stage();
            Scene sc = new Scene(view.root(), 640, 680);
            theme.register(sc);
            s.setScene(sc);
            new SettingsController(view, ctx.app, theme);
            s.show();
            later(0.6, () -> { shot(sc, "12-configuracoes"); s.close(); });
        });
        steps.add(() -> {});
        steps.add(Platform::exit);
        next();
    }

    private void next() {
        Runnable step = steps.poll();
        if (step == null) return;
        try {
            step.run();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        later(0.9, this::next);
    }

    private void later(double seconds, Runnable action) {
        PauseTransition p = new PauseTransition(Duration.seconds(seconds));
        p.setOnFinished(e -> action.run());
        p.play();
    }

    private void shot(Scene sc, String name) {
        WritableImage img = sc.snapshot(null);
        int w = (int) img.getWidth(), h = (int) img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = img.getPixelReader();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setRGB(x, y, reader.getArgb(x, y));
        try {
            ImageIO.write(out, "png", new File("target/preview/" + name + ".png"));
            System.out.println("captura: " + name);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
