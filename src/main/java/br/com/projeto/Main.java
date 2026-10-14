package br.com.projeto;

import br.com.projeto.config.AppConfig;
import br.com.projeto.controller.AuthController;
import br.com.projeto.controller.ChatController;
import br.com.projeto.controller.ThemeController;
import br.com.projeto.repository.ConversationRepository;
import br.com.projeto.repository.DocumentRepository;
import br.com.projeto.repository.MessageRepository;
import br.com.projeto.repository.PasswordResetRepository;
import br.com.projeto.repository.UserRepository;
import br.com.projeto.service.AuthenticationService;
import br.com.projeto.service.ChatService;
import br.com.projeto.service.ConversationService;
import br.com.projeto.service.GroqService;
import br.com.projeto.service.PasswordRecoveryService;
import br.com.projeto.service.RAGService;
import br.com.projeto.util.Ui;
import br.com.projeto.view.Components;
import br.com.projeto.view.SplashView;
import com.mongodb.client.MongoDatabase;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
import java.time.Clock;

/**
 * Ponto de entrada da aplicação.
 *
 * Fluxo: splash (WebView + GSAP) enquanto o MongoDB conecta em segundo plano
 *        → animação de saída → janela principal com a tela de login.
 *
 * Também monta as dependências (Repository → Service → Controller) e faz a navegação entre telas.
 */
public class Main extends Application {

    /** Main usada pelo IntelliJ e pelo instalador: carrega o JavaFX a partir do classpath. */
    public static class Launcher {
        public static void main(String[] args) {
            Application.launch(Main.class, args);
        }
    }

    private Stage stage;
    private Scene scene;
    private SplashView splash;
    final ThemeController theme = new ThemeController();

    // services (criados depois que o banco conecta)
    AuthenticationService auth; // visível no pacote para a pré-visualização das telas
    private ConversationService conversations;
    private RAGService rag;
    private ChatService chat;
    private AuthController authController;

    private String initError;
    private boolean ready, introDone, exitStarted, mainShown, splashAnimated = true;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        splash = new SplashView();
        loadIcons(splash.stage());
        splash.load(new SplashView.Bridge(this::onIntroDone, this::showMainWindow), () -> {
            splashAnimated = false; // WebView indisponível: segue sem a animação
            onIntroDone();
        });
        splash.stage().show();
        splash.setStatus("Conectando ao MongoDB Atlas…", false);
        initialize();
        armWatchdog();
    }

    /** Conecta ao banco em segundo plano, enquanto a animação roda. */
    private void initialize() {
        Ui.async(() -> {
            MongoDatabase db = AppConfig.connect();
            createServices(db);
            rag.seedIfEmpty();
            return true;
        }, ok -> {
            ready = true;
            splash.setStatus("Tudo pronto!", false);
            tryExitSplash();
        }, error -> {
            initError = error;
            splash.setStatus("Falha ao conectar ao banco de dados", true);
            tryExitSplash();
        });
    }

    private void createServices(MongoDatabase db) {
        createServices(new UserRepository(db), new ConversationRepository(db), new MessageRepository(db),
                new DocumentRepository(db), new PasswordResetRepository(db), new GroqService());
    }

    /** Injeção de dependências manual: Repository → Service → Controller. */
    void createServices(UserRepository users, ConversationRepository conversationRepository, MessageRepository messages,
                        DocumentRepository documents, PasswordResetRepository tokens, GroqService groq) {
        auth = new AuthenticationService(users);
        conversations = new ConversationService(conversationRepository, messages, auth);
        rag = new RAGService(documents, auth);
        chat = new ChatService(conversations, messages, rag, groq, auth);
        PasswordRecoveryService recovery = new PasswordRecoveryService(auth, tokens, Clock.systemUTC());
        authController = new AuthController(this, auth, recovery, theme);
    }

    /** Usado só pela pré-visualização das telas (testes): abre direto a janela principal. */
    void startWithoutSplash(Stage primaryStage) {
        stage = primaryStage;
        ready = true;
        showMainWindow();
    }

    private void onIntroDone() {
        introDone = true;
        tryExitSplash();
    }

    /** A splash só sai quando a animação de entrada terminou E o banco respondeu. */
    private void tryExitSplash() {
        if (!introDone || !(ready || initError != null) || exitStarted) return;
        exitStarted = true;
        if (!splashAnimated) {
            showMainWindow();
            return;
        }
        PauseTransition pause = new PauseTransition(Duration.millis(initError == null ? 350 : 1200));
        pause.setOnFinished(e -> splash.call("playExit()")); // o GSAP chama javaBridge.finish() no fim
        pause.play();
    }

    /** Segurança: se a animação travar, a aplicação abre mesmo assim. */
    private void armWatchdog() {
        PauseTransition watchdog = new PauseTransition(Duration.seconds(20));
        watchdog.setOnFinished(e -> {
            if (mainShown) return;
            if (ready || initError != null) showMainWindow(); else armWatchdog();
        });
        watchdog.play();
    }

    private void showMainWindow() {
        if (mainShown || !(ready || initError != null)) return;
        mainShown = true;
        if (splash != null) splash.stage().close();
        scene = new Scene(new StackPane(), 1200, 780);
        theme.register(scene);
        stage.setScene(scene);
        stage.setTitle(AppConfig.APP_NAME);
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        loadIcons(stage);
        if (ready) showLogin(null, null); else showConnectionError();
        stage.centerOnScreen();
        stage.show();
    }

    // ---------- navegação ----------

    public void showLogin(String email, String message) {
        setRoot(authController.loginScreen(email, message), "Entrar");
    }

    public void showRegister() {
        setRoot(authController.registerScreen(), "Criar conta");
    }

    public void showForgotPassword(String email) {
        setRoot(authController.forgotPasswordScreen(email), "Recuperar senha");
    }

    public void showChat() {
        ChatController controller = new ChatController(this, auth, conversations, chat, rag, theme);
        setRoot(controller.view().root(), null);
    }

    /** Abre uma janela secundária (modal) com o mesmo tema. */
    public Stage openWindow(Parent root, String title, double width, double height) {
        Stage window = new Stage();
        window.initOwner(stage);
        window.initModality(Modality.WINDOW_MODAL);
        window.setTitle(title + " — " + AppConfig.APP_NAME);
        window.getIcons().setAll(stage.getIcons());
        Scene windowScene = new Scene(root, width, height);
        Runnable unregister = theme.register(windowScene);
        window.setOnHidden(e -> unregister.run());
        window.setScene(windowScene);
        return window;
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

    /** Tela exibida quando o MongoDB não responde na inicialização. */
    private void showConnectionError() {
        Label message = Components.label(initError, "auth-subtitle");
        message.setWrapText(true);
        Label help = Components.label("Verifique a variável MONGODB_URI, sua conexão com a internet e se o seu IP "
                + "está liberado em Network Access no MongoDB Atlas. Consulte o README.", "muted");
        help.setWrapText(true);
        Components.LoadingButton retry = new Components.LoadingButton("Tentar novamente");
        retry.setMaxWidth(200);
        Button exit = new Button("Sair");
        exit.getStyleClass().add("secondary-button");
        exit.setOnAction(e -> Platform.exit());
        retry.setOnAction(e -> {
            retry.setLoading(true, "Conectando…");
            AppConfig.disconnect();
            AppConfig.reload();
            Ui.async(() -> {
                createServices(AppConfig.connect());
                rag.seedIfEmpty();
                return true;
            }, ok -> {
                ready = true;
                showLogin(null, null);
            }, error -> {
                retry.setLoading(false, null);
                message.setText(error);
            });
        });
        StackPane icon = new StackPane(Components.icon(Components.ALERT, 28));
        icon.getStyleClass().add("error-icon");
        VBox card = new VBox(16, icon, Components.label("Não foi possível iniciar", "auth-title"), message, help,
                new HBox(10, retry, exit));
        card.getStyleClass().add("auth-card");
        card.setMaxSize(520, VBox.USE_PREF_SIZE);
        StackPane root = new StackPane(card);
        root.getStyleClass().add("auth-root");
        root.setPadding(new Insets(32));
        setRoot(root, "Erro de conexão");
    }

    private void loadIcons(Stage target) {
        for (String size : new String[]{"16", "32", "64", "256"}) {
            InputStream in = getClass().getResourceAsStream("/images/icon-" + size + ".png");
            if (in != null) target.getIcons().add(new Image(in));
        }
    }

    @Override
    public void stop() {
        Ui.shutdown();
        AppConfig.disconnect();
    }

    public static void main(String[] args) {
        Launcher.main(args);
    }
}
