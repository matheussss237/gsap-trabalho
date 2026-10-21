package com.matheushenrique.lumi.view;

import com.matheushenrique.lumi.exceptions.GlobalExceptionHandler;
import javafx.animation.*;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

// peças reutilizadas pelas telas: ícones, logo, campos, avisos, botões, diálogos e tarefas em segundo plano
public class Componentes {

    // ícones em SVG (a cor vem do CSS e muda com o tema)
    public static final String MAIS = "M11 5h2v14h-2zM5 11h14v2H5z";
    public static final String ENVIAR = "M2 21l21-9L2 3v7l15 2-15 2z";
    public static final String LIXEIRA = "M6 7h12l-1 14H7L6 7zm3-4h6l1 2h4v2H4V5h4l1-2z";
    public static final String LAPIS = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41"
            + "l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    public static final String SAIR = "M10 17l1.4-1.4-2.6-2.6H20v-2H8.8l2.6-2.6L10 7l-5 5 5 5zM4 3h8v2H4v14h8v2H4"
            + "a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z";
    public static final String SOL = "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zM11 1h2v3h-2zM11 20h2v3h-2zM1 11h3v2H1z"
            + "M20 11h3v2h-3zM4.2 5.6l1.4-1.4 2.1 2.1-1.4 1.4zM16.3 17.7l1.4-1.4 2.1 2.1-1.4 1.4zM4.2 18.4l2.1-2.1"
            + " 1.4 1.4-2.1 2.1zM16.3 6.3l2.1-2.1 1.4 1.4-2.1 2.1z";
    public static final String LUA = "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z";
    public static final String LIVRO = "M4 4.5A2.5 2.5 0 0 1 6.5 2H20v17H6.5a1.5 1.5 0 0 0 0 3H20v2H6.5A3.5 3.5 0 0 1"
            + " 3 20.5v-16zM7 6v2h9V6z";
    public static final String MENU = "M3 6h18v2H3zM3 11h18v2H3zM3 16h18v2H3z";
    public static final String COPIAR = "M16 1H4a2 2 0 0 0-2 2v14h2V3h12V1zm3 4H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11"
            + "a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H8V7h11v14z";
    public static final String LUPA = "M15.5 14h-.8l-.3-.3A6.5 6.5 0 1 0 14 15.5l.3.3v.8l5 5 1.5-1.5-5-5zm-6 0"
            + "A4.5 4.5 0 1 1 14 9.5 4.5 4.5 0 0 1 9.5 14z";
    public static final String BALAO = "M4 3h16a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H8l-5 4V5a2 2 0 0 1 2-2z";
    public static final String BRILHO = "M12 2l2.2 6.8L21 11l-6.8 2.2L12 20l-2.2-6.8L3 11l6.8-2.2z";
    public static final String CERTO = "M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z";
    public static final String DOCUMENTO = "M6 2h8l6 6v14H4V2h2zm7 1.5V9h5.5L13 3.5z";
    public static final String ALERTA = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";

    private static final ExecutorService TAREFAS = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "lumi-tarefa");
        thread.setDaemon(true);
        return thread;
    });

    private Componentes() {
    }

    // roda MongoDB/Groq fora da thread da tela (para não travar) e volta para a tela no final
    public static <T> void emSegundoPlano(Callable<T> trabalho, Consumer<T> sucesso, Consumer<String> erro) {
        Task<T> tarefa = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabalho.call();
            }
        };
        tarefa.setOnSucceeded(e -> sucesso.accept(tarefa.getValue()));
        tarefa.setOnFailed(e -> erro.accept(GlobalExceptionHandler.mensagem(tarefa.getException())));
        TAREFAS.submit(tarefa);
    }

    public static void encerrarTarefas() {
        TAREFAS.shutdownNow();
    }

    // "14:32" se for hoje, "12/10/2026 14:32" se for outro dia
    public static String formatarData(LocalDateTime data) {
        if (data == null) return "";
        String padrao = data.toLocalDate().equals(LocalDate.now()) ? "HH:mm" : "dd/MM/yyyy HH:mm";
        return data.format(DateTimeFormatter.ofPattern(padrao));
    }

    public static Region icone(String svg, double tamanho) {
        Region icone = new Region();
        icone.getStyleClass().add("icon");
        icone.setStyle("-fx-shape: \"" + svg + "\";");
        icone.setMinSize(tamanho, tamanho);
        icone.setPrefSize(tamanho, tamanho);
        icone.setMaxSize(tamanho, tamanho);
        return icone;
    }

    public static Button botaoIcone(String svg, String dica) {
        Button botao = new Button(null, icone(svg, 15));
        botao.getStyleClass().add("icon-button");
        botao.setTooltip(new Tooltip(dica));
        return botao;
    }

    public static Label texto(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }

    // logo da Lumi (igual ao da animação GSAP): um "L" de pontos ligados + um ponto de luz
    public static StackPane logo(double tamanho) {
        double s = tamanho / 120.0;
        Rectangle fundo = new Rectangle(112 * s, 112 * s);
        fundo.setArcWidth(60 * s);
        fundo.setArcHeight(60 * s);
        fundo.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7c5cff")), new Stop(1, Color.web("#22d3ee"))));
        Polyline letra = new Polyline(40 * s, 30 * s, 40 * s, 86 * s, 84 * s, 86 * s);
        letra.setStroke(Color.WHITE);
        letra.setStrokeWidth(10 * s);
        letra.setStrokeLineCap(StrokeLineCap.ROUND);
        letra.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Group desenho = new Group(letra,
                new Circle(40 * s, 30 * s, 9.5 * s, Color.WHITE),
                new Circle(40 * s, 86 * s, 9.5 * s, Color.WHITE),
                new Circle(84 * s, 86 * s, 9.5 * s, Color.WHITE),
                new Circle(82 * s, 38 * s, 7 * s, Color.web("#fff6b3")));
        StackPane logo = new StackPane(fundo, desenho);
        logo.setMinSize(tamanho, tamanho);
        logo.setMaxSize(tamanho, tamanho);
        return logo;
    }

    // bolinha com a inicial do nome
    public static StackPane avatar(String nome, double tamanho) {
        String inicial = nome == null || nome.isBlank() ? "?" : nome.trim().substring(0, 1).toUpperCase();
        StackPane avatar = new StackPane(texto(inicial, "avatar-letter"));
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(tamanho, tamanho);
        avatar.setMaxSize(tamanho, tamanho);
        return avatar;
    }

    // ---------- diálogos (usam o mesmo tema da janela) ----------

    public static boolean confirmar(Window dono, String titulo, String mensagem, String textoBotao) {
        ButtonType sim = new ButtonType(textoBotao, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensagem, sim, cancelar);
        alerta.setHeaderText(titulo);
        prepararDialogo(alerta, dono, "Confirmação");
        alerta.getDialogPane().lookupButton(sim).getStyleClass().add("danger-button");
        return alerta.showAndWait().filter(sim::equals).isPresent();
    }

    public static Optional<String> perguntarTexto(Window dono, String titulo, String rotulo, String valorInicial) {
        TextInputDialog dialogo = new TextInputDialog(valorInicial);
        dialogo.setHeaderText(titulo);
        dialogo.setContentText(rotulo);
        prepararDialogo(dialogo, dono, titulo);
        ((Button) dialogo.getDialogPane().lookupButton(ButtonType.OK)).setText("Salvar");
        ((Button) dialogo.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        return dialogo.showAndWait().map(String::trim);
    }

    public static void mostrarErro(Window dono, String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensagem, ButtonType.OK);
        alerta.setHeaderText(titulo);
        prepararDialogo(alerta, dono, "Erro");
        alerta.showAndWait();
    }

    private static void prepararDialogo(Dialog<?> dialogo, Window dono, String tituloJanela) {
        dialogo.setTitle(tituloJanela);
        if (dono != null) {
            dialogo.initOwner(dono);
            dialogo.getDialogPane().getStylesheets().setAll(dono.getScene().getStylesheets());
        }
    }

    // ---------- campo de formulário: rótulo + entrada + mensagem de erro ----------
    public static class Campo extends VBox {

        private final TextInputControl entrada;
        private final Label erro = new Label();

        public Campo(String rotulo, String dica, boolean senha) {
            super(6);
            entrada = senha ? new PasswordField() : new TextField();
            entrada.setPromptText(dica);
            entrada.getStyleClass().add("form-input");
            entrada.textProperty().addListener((obs, antigo, novo) -> limparErro());
            erro.getStyleClass().add("field-error");
            erro.setWrapText(true);
            erro.managedProperty().bind(erro.visibleProperty());
            erro.setVisible(false);
            getChildren().addAll(texto(rotulo, "form-label"), entrada, erro);
        }

        public TextInputControl getEntrada() { return entrada; }
        public String getValor() { return entrada.getText(); }

        public void mostrarErro(String mensagem) {
            erro.setText(mensagem);
            erro.setVisible(true);
            if (!entrada.getStyleClass().contains("invalid")) entrada.getStyleClass().add("invalid");
        }

        public void limparErro() {
            erro.setVisible(false);
            entrada.getStyleClass().remove("invalid");
        }
    }

    // ---------- faixa de aviso (erro, sucesso ou informação) ----------
    public static class Aviso extends HBox {

        private final Label mensagem = new Label();
        private final Region icone = icone(ALERTA, 14);

        public Aviso() {
            super(10);
            getStyleClass().add("banner");
            mensagem.setWrapText(true);
            mensagem.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(mensagem, Priority.ALWAYS);
            getChildren().addAll(icone, mensagem);
            managedProperty().bind(visibleProperty());
            setVisible(false);
        }

        public void erro(String texto) { mostrar("banner-error", ALERTA, texto); }
        public void sucesso(String texto) { mostrar("banner-success", CERTO, texto); }
        public void info(String texto) { mostrar("banner-info", ALERTA, texto); }
        public void esconder() { setVisible(false); }

        private void mostrar(String estilo, String svg, String texto) {
            getStyleClass().removeAll("banner-error", "banner-success", "banner-info");
            getStyleClass().add(estilo);
            icone.setStyle("-fx-shape: \"" + svg + "\";");
            mensagem.setText(texto);
            setVisible(true);
            FadeTransition fade = new FadeTransition(Duration.millis(180), this);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }

    // ---------- botão que mostra um "carregando" enquanto espera ----------
    public static class BotaoCarregando extends Button {

        private final String textoNormal;
        private final ProgressIndicator girando = new ProgressIndicator();

        public BotaoCarregando(String texto) {
            super(texto);
            textoNormal = texto;
            girando.getStyleClass().add("button-spinner");
            girando.setMaxSize(16, 16);
            getStyleClass().add("primary-button");
            setMaxWidth(Double.MAX_VALUE);
        }

        public void carregando(String texto) {
            setDisable(true);
            setGraphic(girando);
            setText(texto);
        }

        public void pronto() {
            setDisable(false);
            setGraphic(null);
            setText(textoNormal);
        }
    }

    // ---------- "a Lumi está pensando..." com três pontinhos animados ----------
    public static class Digitando extends HBox {

        private final Timeline animacao = new Timeline();

        public Digitando() {
            super(5);
            getStyleClass().add("typing-indicator");
            setAlignment(Pos.CENTER_LEFT);
            for (int i = 0; i < 3; i++) {
                Circle ponto = new Circle(4);
                ponto.getStyleClass().add("typing-dot");
                getChildren().add(ponto);
                Duration inicio = Duration.millis(i * 160);
                animacao.getKeyFrames().addAll(
                        new KeyFrame(inicio, new KeyValue(ponto.opacityProperty(), 0.3), new KeyValue(ponto.translateYProperty(), 0)),
                        new KeyFrame(inicio.add(Duration.millis(300)), new KeyValue(ponto.opacityProperty(), 1),
                                new KeyValue(ponto.translateYProperty(), -4)),
                        new KeyFrame(inicio.add(Duration.millis(600)), new KeyValue(ponto.opacityProperty(), 0.3),
                                new KeyValue(ponto.translateYProperty(), 0)));
            }
            animacao.getKeyFrames().add(new KeyFrame(Duration.millis(1100)));
            animacao.setCycleCount(Animation.INDEFINITE);
            getChildren().add(texto("A Lumi está consultando a base de conhecimento…", "typing-label"));
            managedProperty().bind(visibleProperty());
            setVisible(false);
        }

        public void mostrar(boolean ativo) {
            setVisible(ativo);
            if (ativo) animacao.play(); else animacao.stop();
        }
    }
}
