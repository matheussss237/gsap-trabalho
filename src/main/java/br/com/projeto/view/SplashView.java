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
 * Janela transparente e sem bordas que carrega resources/splash/index.html.
 *
 * Comunicação: Java → JS com setStatus(...) e playExit();
 *              JS → Java com javaBridge.introDone() e javaBridge.finish().
 */
public class SplashView {

    /** Objeto exposto ao JavaScript como "window.javaBridge" (métodos precisam ser públicos). */
    public static class Bridge {
        private final Runnable onIntroDone;
        private final Runnable onFinish;

        public Bridge(Runnable onIntroDone, Runnable onFinish) {
            this.onIntroDone = onIntroDone;
            this.onFinish = onFinish;
        }

        public void introDone() { onIntroDone.run(); }
        public void finish() { onFinish.run(); }
    }

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private Bridge bridge; // campo: o WebEngine só guarda referência fraca e o GC poderia removê-lo
    private boolean ready;
    private String pendingStatus;

    public SplashView() {
        webView.setPageFill(Color.TRANSPARENT);
        webView.setContextMenuEnabled(false);
        stage.setScene(new Scene(webView, 760, 460, Color.TRANSPARENT));
        stage.setTitle("Lumi");
        stage.centerOnScreen();
    }

    /** Carrega a página; quando pronta, registra a ponte e dispara a animação de entrada. */
    public void load(Bridge javaBridge, Runnable onLoadFailed) {
        this.bridge = javaBridge;
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
                window.setMember("__javafx", true);
                engine.executeScript("startIntro()");
                ready = true;
                if (pendingStatus != null) call(pendingStatus);
            } else if (state == Worker.State.FAILED) {
                onLoadFailed.run();
            }
        });
        URL page = getClass().getResource("/splash/index.html");
        if (page == null) {
            onLoadFailed.run();
        } else {
            engine.load(page.toExternalForm());
        }
    }

    public void call(String script) {
        try {
            engine.executeScript(script);
        } catch (RuntimeException e) {
            System.err.println("Falha ao executar script na splash: " + e.getMessage());
        }
    }

    public void setStatus(String text, boolean error) {
        String script = "setStatus('" + text.replace("\\", "\\\\").replace("'", "\\'") + "', " + error + ")";
        if (ready) call(script); else pendingStatus = script; // a página ainda não carregou
    }

    public Stage stage() {
        return stage;
    }
}
