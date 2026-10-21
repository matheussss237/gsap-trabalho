package com.matheushenrique.lumi;

import com.matheushenrique.lumi.controller.TemaController;
import com.matheushenrique.lumi.dto.CadastroDTO;
import com.sun.net.httpserver.HttpServer;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/*
 * Ferramenta de desenvolvimento (não faz parte do app): roda a Lumi inteira (splash, Spring, telas)
 * com um MongoDB falso e uma Groq falsa, e salva prints em target/preview. Uso: scripts/preview-ui.sh
 */
public class PreviewTelas extends Main {

    public static void main(String[] args) throws Exception {
        InetSocketAddress mongo = new MongoServer(new MemoryBackend()).bind();
        System.setProperty("MONGODB_URI", "mongodb://" + mongo.getHostString() + ":" + mongo.getPort() + "/lumi");

        String resposta = """
                ## Encapsulamento em Java

                Boa pergunta, Matheus! Encapsulamento é **esconder o estado interno** de um objeto.

                - atributos `private`
                - acesso por métodos públicos

                ```java
                public class Conta {
                    private double saldo;
                }
                ```""";
        HttpServer groq = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        groq.createContext("/", troca -> {
            com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
            msg.addProperty("content", resposta);
            String corpo = "{\"choices\":[{\"message\":" + msg + "}]}";
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(200, bytes.length);
            try (OutputStream saida = troca.getResponseBody()) { saida.write(bytes); }
        });
        groq.start();
        System.setProperty("GROQ_API_KEY", "chave-falsa");
        System.setProperty("groq.url", "http://127.0.0.1:" + groq.getAddress().getPort() + "/");
        Application.launch(PreviewTelas.class, args);
    }

    private final Deque<Runnable> passos = new ArrayDeque<>();
    private Stage palco;

    @Override
    public void start(Stage janelaPrincipal) {
        new File("target/preview").mkdirs();
        palco = janelaPrincipal;
        temaController.mudarTema(TemaController.Tema.ESCURO);
        super.start(janelaPrincipal);
        PauseTransition meio = new PauseTransition(Duration.seconds(2.5));
        meio.setOnFinished(e -> Window.getWindows().stream().filter(Window::isShowing).findFirst()
                .ifPresent(w -> print(w.getScene(), "01-splash")));
        meio.play();

        passos.add(() -> print(palco.getScene(), "02-login-escuro"));
        passos.add(() -> temaController.mudarTema(TemaController.Tema.CLARO));
        passos.add(() -> print(palco.getScene(), "03-login-claro"));
        passos.add(this::mostrarCadastro);
        passos.add(() -> print(palco.getScene(), "04-cadastro"));
        passos.add(() -> mostrarRecuperarSenha("matheus@empresa.com"));
        passos.add(() -> print(palco.getScene(), "05-recuperar-senha"));
        passos.add(() -> {
            usuarioService.cadastrarUsuario(new CadastroDTO("Matheus Henrique", "matheus@empresa.com", "senha1234", "senha1234"));
            usuarioService.fazerLogin("matheus@empresa.com", "senha1234");
            temaController.mudarTema(TemaController.Tema.ESCURO);
            mostrarChat();
        });
        passos.add(() -> print(palco.getScene(), "06-chat-vazio"));
        passos.add(() -> {
            ((TextArea) palco.getScene().lookup(".composer-input")).setText("Explique o encapsulamento em Java");
            ((Button) palco.getScene().lookup(".send-button")).fire(); // envio de verdade: ChatService -> RAG -> Groq
        });
        passos.add(() -> { });
        passos.add(() -> print(palco.getScene(), "07-chat-escuro"));
        passos.add(() -> temaController.mudarTema(TemaController.Tema.CLARO));
        passos.add(() -> print(palco.getScene(), "08-chat-claro"));
        passos.add(() -> palco.setWidth(780));
        passos.add(() -> print(palco.getScene(), "09-chat-estreito"));
        passos.add(() -> {
            palco.setWidth(1200);
            Button base = (Button) palco.getScene().getRoot().lookupAll(".sidebar-link").iterator().next();
            Platform.runLater(base::fire);
        });
        passos.add(() -> print(outraJanela().getScene(), "10-base-conhecimento"));
        passos.add(() -> {
            Scene cena = outraJanela().getScene();
            ((TabPane) cena.lookup(".tab-pane")).getSelectionModel().select(1);
            TextField campo = (TextField) cena.getRoot().lookupAll(".text-field").stream()
                    .filter(n -> String.valueOf(((TextField) n).getPromptText()).startsWith("Ex.: Qual")).findFirst().orElseThrow();
            campo.setText("Qual é o horário do suporte técnico da empresa?");
            campo.fireEvent(new javafx.event.ActionEvent());
        });
        passos.add(() -> print(outraJanela().getScene(), "11-teste-rag"));
        passos.add(() -> ((Stage) outraJanela()).close());
        passos.add(() -> System.exit(0));
        esperarLogin();
    }

    // espera a splash terminar e o login aparecer
    private void esperarLogin() {
        PauseTransition espera = new PauseTransition(Duration.seconds(1));
        espera.setOnFinished(e -> {
            if (palco.isShowing() && palco.getScene() != null && palco.getScene().lookup(".auth-card") != null) {
                proximo();
            } else {
                esperarLogin();
            }
        });
        espera.play();
    }

    private void proximo() {
        Runnable passo = passos.poll();
        if (passo == null) return;
        PauseTransition pausa = new PauseTransition(Duration.seconds(1.2));
        pausa.setOnFinished(e -> {
            try {
                passo.run();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            proximo();
        });
        pausa.play();
    }

    private Window outraJanela() {
        return Window.getWindows().stream().filter(w -> w != palco && w.isShowing()).findFirst().orElseThrow();
    }

    private void print(Scene cena, String nome) {
        WritableImage imagem = cena.snapshot(null);
        int w = (int) imagem.getWidth(), h = (int) imagem.getHeight();
        BufferedImage saida = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixels = imagem.getPixelReader();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) saida.setRGB(x, y, pixels.getArgb(x, y));
        try {
            ImageIO.write(saida, "png", new File("target/preview/" + nome + ".png"));
            System.out.println("print: " + nome);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    // inicia com o JavaFX vindo do classpath
    public static class Iniciar {
        public static void main(String[] args) throws Exception {
            PreviewTelas.main(args);
        }
    }
}
