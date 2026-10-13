package br.com.projeto.view;

import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

import java.net.URL;

/**
 * Splash screen: JavaFX → WebView → HTML + CSS + JavaScript → GSAP.
 * A janela é transparente e sem bordas; o conteúdo é a página resources/splash/index.html.
 */
public class SplashView {

    public static final double WIDTH = 760;
    public static final double HEIGHT = 460;

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();

    /**
     * Ponte JS → Java. Precisa ser mantida em um campo: o WebEngine guarda apenas
     * referência fraca e o objeto poderia ser coletado pelo GC.
     */
    private Object bridge;
    private boolean ready;
    private String pendingStatus;

    public SplashView() {
        webView.setPageFill(Color.TRANSPARENT);
        webView.setContextMenuEnabled(false);
        webView.setPrefSize(WIDTH, HEIGHT);

        Scene scene = new Scene(webView, WIDTH, HEIGHT, Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Nexus Chat");
        stage.centerOnScreen();
    }

    /** Carrega a página; quando pronta, registra a ponte e dispara a animação de entrada. */
    public void load(Object javaBridge, Runnable onLoadFailed) {
        this.bridge = javaBridge;
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
                window.setMember("__javafx", true);
                engine.executeScript("startIntro()");
                ready = true;
                if (pendingStatus != null) {
                    call(pendingStatus);
                }
            } else if (state == Worker.State.FAILED) {
                onLoadFailed.run();
            }
        });
        URL page = getClass().getResource("/splash/index.html");
        if (page == null) {
            onLoadFailed.run();
            return;
        }
        engine.load(page.toExternalForm());
    }

    /** Chama uma função JavaScript da página (ex.: setStatus, playExit). */
    public void call(String script) {
        try {
            engine.executeScript(script);
        } catch (RuntimeException e) {
            System.err.println("Falha ao executar script na splash: " + e.getMessage());
        }
    }

    public void setStatus(String text, boolean error) {
        String escaped = text.replace("\\", "\\\\").replace("'", "\\'");
        String script = "setStatus('" + escaped + "', " + error + ")";
        if (ready) {
            call(script);
        } else {
            pendingStatus = script; // a página ainda não carregou: aplica depois
        }
    }

    public Stage stage() {
        return stage;
    }
}
