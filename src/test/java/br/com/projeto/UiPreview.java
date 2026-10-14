package br.com.projeto;

import br.com.projeto.controller.ThemeController;
import br.com.projeto.view.SplashView;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Ferramenta de desenvolvimento (não faz parte do app): abre as telas com dados em memória
 * e salva capturas em target/preview. Uso: scripts/preview-ui.sh (Linux + Xvfb).
 */
public class UiPreview extends Application {

    public static void main(String[] args) {
        Application.launch(UiPreview.class, args);
    }

    private final TestSupport data = new TestSupport();
    private final Deque<Runnable> steps = new ArrayDeque<>();
    private final Main app = new Main();
    private Stage stage;

    @Override
    public void start(Stage primary) {
        new File("target/preview").mkdirs();
        stage = primary;
        app.theme.setTheme(ThemeController.Theme.DARK);
        data.rag.seedIfEmpty();
        data.groq.answer = """
                ## Encapsulamento em Java

                Boa pergunta, Matheus! Encapsulamento é o princípio que **esconde o estado interno** de um objeto.

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
        SplashView splash = new SplashView();
        splash.load(new SplashView.Bridge(() -> {
            shot(splash.stage().getScene(), "01-splash");
            splash.call("playExit()");
        }, () -> {
            splash.stage().close();
            mainScreens();
        }), () -> System.err.println("SPLASH FALHOU"));
        splash.stage().show();
        splash.setStatus("Conectando ao MongoDB Atlas…", false);
    }

    private void mainScreens() {
        app.createServices(data.users, data.conversationRepo, data.messageRepo, data.documentRepo, data.tokens, data.groq);
        app.startWithoutSplash(stage);
        steps.add(() -> shot(stage.getScene(), "02-login-escuro"));
        steps.add(() -> app.theme.setTheme(ThemeController.Theme.LIGHT));
        steps.add(() -> shot(stage.getScene(), "03-login-claro"));
        steps.add(app::showRegister);
        steps.add(() -> shot(stage.getScene(), "04-cadastro"));
        steps.add(() -> app.showForgotPassword("matheus@empresa.com"));
        steps.add(() -> shot(stage.getScene(), "05-recuperar-senha"));
        steps.add(() -> {
            data.auth.register("Matheus Henrique", "matheus@empresa.com", "senha1234", "senha1234");
            data.auth.login("matheus@empresa.com", "senha1234");
            data.chat.send(data.conversations.create().getId(), "Explique o encapsulamento em Java");
            data.conversations.rename(data.conversations.create().getId(), "Estudando MongoDB");
            data.auth.logout();
            app.auth.login("matheus@empresa.com", "senha1234");
            app.theme.setTheme(ThemeController.Theme.DARK);
            app.showChat();
        });
        steps.add(() -> shot(stage.getScene(), "06-chat-vazio"));
        steps.add(() -> ((ListView<?>) stage.getScene().lookup(".list-view")).getSelectionModel().select(1));
        steps.add(() -> shot(stage.getScene(), "07-chat-escuro"));
        steps.add(() -> app.theme.setTheme(ThemeController.Theme.LIGHT));
        steps.add(() -> shot(stage.getScene(), "08-chat-claro"));
        steps.add(() -> stage.setWidth(780));
        steps.add(() -> shot(stage.getScene(), "09-chat-estreito"));
        steps.add(() -> {
            stage.setWidth(1200);
            Button kb = (Button) stage.getScene().getRoot().lookupAll(".sidebar-link").iterator().next();
            Platform.runLater(kb::fire); // abre a base de conhecimento (janela modal)
        });
        steps.add(() -> shot(dialog().getScene(), "10-base-conhecimento"));
        steps.add(() -> {
            Scene sc = dialog().getScene();
            ((TabPane) sc.lookup(".tab-pane")).getSelectionModel().select(1);
            TextField query = (TextField) sc.getRoot().lookupAll(".text-field").stream()
                    .filter(n -> ((TextField) n).getPromptText().startsWith("Ex.: Qual")).findFirst().orElseThrow();
            query.setText("Qual é o horário do suporte técnico da empresa?");
            query.fireEvent(new javafx.event.ActionEvent());
        });
        steps.add(() -> shot(dialog().getScene(), "11-teste-rag"));
        steps.add(() -> ((Stage) dialog()).close());
        steps.add(Platform::exit);
        next();
    }

    private Window dialog() {
        return Window.getWindows().stream().filter(w -> w != stage && w.isShowing()).findFirst().orElseThrow();
    }

    private void next() {
        Runnable step = steps.poll();
        if (step == null) return;
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            try {
                step.run();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            next();
        });
        pause.play();
    }

    private void shot(Scene scene, String name) {
        WritableImage img = scene.snapshot(null);
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

    /** Main com o JavaFX no classpath. */
    public static class Launcher {
        public static void main(String[] args) {
            Application.launch(UiPreview.class, args);
        }
    }

}
