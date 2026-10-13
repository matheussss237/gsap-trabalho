package br.com.projeto;

import br.com.projeto.config.AppConfig;
import br.com.projeto.config.AppContext;
import br.com.projeto.config.MongoConnection;
import br.com.projeto.controller.ThemeController;
import br.com.projeto.service.ThemeService;
import br.com.projeto.util.FxAsync;
import br.com.projeto.view.ConnectionErrorView;
import br.com.projeto.view.Navigator;
import br.com.projeto.view.SplashBridge;
import br.com.projeto.view.SplashView;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;

/**
 * Ponto de entrada JavaFX.
 *
 * Fluxo: Splash (WebView + GSAP) enquanto o MongoDB conecta em segundo plano
 *        → animação de saída → janela principal com a tela de Login.
 */
public class Main extends Application {

    private Stage primaryStage;
    private SplashView splash;
    private ThemeController themeController;

    private AppContext context;
    private String initError;
    private boolean splashAnimated = true;
    private boolean introDone;
    private boolean exitStarted;
    private boolean mainShown;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.themeController = new ThemeController(new ThemeService());

        splash = new SplashView();
        loadIcons(splash.stage());
        SplashBridge bridge = new SplashBridge(this::onIntroDone, this::showMainWindow);
        splash.load(bridge, () -> {
            // WebView/GSAP indisponível: segue sem a animação
            splashAnimated = false;
            onIntroDone();
        });
        splash.stage().show();
        splash.setStatus("Conectando ao MongoDB Atlas…", false);

        // conexão com o banco em paralelo à animação
        initializeContext();
        armWatchdog();
    }

    private void initializeContext() {
        FxAsync.run(AppContext::initialize, ctx -> {
            context = ctx;
            splash.setStatus("Tudo pronto!", false);
            tryExitSplash();
        }, error -> {
            initError = error;
            splash.setStatus("Falha ao conectar ao banco de dados", true);
            tryExitSplash();
        });
    }

    /** Segurança: se a animação não responder, a aplicação abre mesmo assim. */
    private void armWatchdog() {
        PauseTransition watchdog = new PauseTransition(Duration.seconds(20));
        watchdog.setOnFinished(e -> {
            if (!mainShown && initFinished()) {
                showMainWindow();
            } else if (!mainShown) {
                armWatchdog();
            }
        });
        watchdog.play();
    }

    private void onIntroDone() {
        introDone = true;
        tryExitSplash();
    }

    private boolean initFinished() {
        return context != null || initError != null;
    }

    /** Inicia a animação de saída quando a entrada terminou E o banco respondeu. */
    private void tryExitSplash() {
        if (!introDone || !initFinished() || exitStarted) {
            return;
        }
        exitStarted = true;
        if (!splashAnimated) {
            showMainWindow();
            return;
        }
        // pequena pausa para o usuário ler o status antes da saída
        PauseTransition pause = new PauseTransition(Duration.millis(initError == null ? 350 : 1200));
        pause.setOnFinished(e -> splash.call("playExit()"));
        pause.play();
    }

    /** Fecha a splash e abre a janela principal (chamado pelo GSAP ao fim da saída). */
    private void showMainWindow() {
        if (mainShown || !initFinished()) {
            return;
        }
        mainShown = true;
        splash.stage().close();

        primaryStage.setTitle(AppConfig.APP_NAME);
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(560);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(780);
        loadIcons(primaryStage);

        Scene scene = new Scene(new StackPane(), 1200, 780);
        themeController.register(scene);
        primaryStage.setScene(scene);

        if (context != null) {
            new Navigator(primaryStage, scene, context, themeController).showLogin(null, null);
        } else {
            showConnectionError(scene);
        }
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private void showConnectionError(Scene scene) {
        ConnectionErrorView view = new ConnectionErrorView(initError);
        view.exitButton().setOnAction(e -> Platform.exit());
        view.retryButton().setOnAction(e -> {
            view.retryButton().setLoading(true, "Conectando…");
            MongoConnection.close();
            AppConfig.reload();
            FxAsync.run(AppContext::initialize, ctx -> {
                context = ctx;
                new Navigator(primaryStage, scene, context, themeController).showLogin(null, null);
            }, error -> {
                view.retryButton().setLoading(false);
                view.setMessage(error);
            });
        });
        scene.setRoot(view.root());
    }

    private void loadIcons(Stage stage) {
        for (String size : new String[]{"16", "32", "64", "256"}) {
            InputStream in = getClass().getResourceAsStream("/images/icon-" + size + ".png");
            if (in != null) {
                stage.getIcons().add(new Image(in));
            }
        }
    }

    @Override
    public void stop() {
        FxAsync.shutdown();
        MongoConnection.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
