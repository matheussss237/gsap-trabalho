package com.matheushenrique.lumi.view;

import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

import java.net.URL;

/*
 * Tela de abertura: JavaFX -> WebView -> HTML + CSS + JavaScript -> GSAP
 * Java -> JS: setStatus(...) e playExit()
 * JS -> Java: javaBridge.introDone() e javaBridge.finish()
 */
public class SplashView {

    // objeto que o JavaScript enxerga como "window.javaBridge" (os métodos precisam ser públicos)
    public static class Ponte {
        private final Runnable aoTerminarEntrada;
        private final Runnable aoTerminarSaida;

        public Ponte(Runnable aoTerminarEntrada, Runnable aoTerminarSaida) {
            this.aoTerminarEntrada = aoTerminarEntrada;
            this.aoTerminarSaida = aoTerminarSaida;
        }

        public void introDone() { aoTerminarEntrada.run(); }
        public void finish() { aoTerminarSaida.run(); }
    }

    private final Stage janela = new Stage(StageStyle.TRANSPARENT);
    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private Ponte ponte; // fica num campo para o Java não apagar o objeto da memória
    private boolean carregada;
    private String statusPendente;

    public SplashView() {
        webView.setPageFill(Color.TRANSPARENT);
        webView.setContextMenuEnabled(false);
        janela.setScene(new Scene(webView, 760, 460, Color.TRANSPARENT));
        janela.setTitle("Lumi");
        janela.centerOnScreen();
    }

    public void carregar(Ponte ponte, Runnable seDerErro) {
        this.ponte = ponte;
        engine.getLoadWorker().stateProperty().addListener((obs, antigo, estado) -> {
            if (estado == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", this.ponte);
                window.setMember("__javafx", true);
                engine.executeScript("startIntro()");
                carregada = true;
                if (statusPendente != null) executar(statusPendente);
            } else if (estado == Worker.State.FAILED) {
                seDerErro.run();
            }
        });
        URL pagina = getClass().getResource("/splash/index.html");
        if (pagina == null) {
            seDerErro.run();
        } else {
            engine.load(pagina.toExternalForm());
        }
    }

    public void executar(String script) {
        try {
            engine.executeScript(script);
        } catch (RuntimeException e) {
            System.err.println("Erro no script da splash: " + e.getMessage());
        }
    }

    public void mostrarStatus(String texto, boolean erro) {
        String script = "setStatus('" + texto.replace("'", "\\'") + "', " + erro + ")";
        if (carregada) executar(script); else statusPendente = script;
    }

    public Stage getJanela() {
        return janela;
    }
}
