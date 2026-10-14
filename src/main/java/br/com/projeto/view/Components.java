package br.com.projeto.view;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

/** Componentes visuais reutilizados pelas telas (ícones, logotipo, campos, avisos, botões). */
public final class Components {

    // Ícones em SVG path; a cor vem do CSS (.icon { -fx-background-color }) e acompanha o tema.
    public static final String PLUS = "M11 5h2v14h-2zM5 11h14v2H5z";
    public static final String SEND = "M2 21l21-9L2 3v7l15 2-15 2z";
    public static final String TRASH = "M6 7h12l-1 14H7L6 7zm3-4h6l1 2h4v2H4V5h4l1-2z";
    public static final String EDIT = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41"
            + "l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    public static final String LOGOUT = "M10 17l1.4-1.4-2.6-2.6H20v-2H8.8l2.6-2.6L10 7l-5 5 5 5zM4 3h8v2H4v14h8v2H4"
            + "a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z";
    public static final String SUN = "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zM11 1h2v3h-2zM11 20h2v3h-2zM1 11h3v2H1z"
            + "M20 11h3v2h-3zM4.2 5.6l1.4-1.4 2.1 2.1-1.4 1.4zM16.3 17.7l1.4-1.4 2.1 2.1-1.4 1.4zM4.2 18.4l2.1-2.1"
            + " 1.4 1.4-2.1 2.1zM16.3 6.3l2.1-2.1 1.4 1.4-2.1 2.1z";
    public static final String MOON = "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z";
    public static final String BOOK = "M4 4.5A2.5 2.5 0 0 1 6.5 2H20v17H6.5a1.5 1.5 0 0 0 0 3H20v2H6.5A3.5 3.5 0 0 1"
            + " 3 20.5v-16zM7 6v2h9V6z";
    public static final String MENU = "M3 6h18v2H3zM3 11h18v2H3zM3 16h18v2H3z";
    public static final String COPY = "M16 1H4a2 2 0 0 0-2 2v14h2V3h12V1zm3 4H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11"
            + "a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H8V7h11v14z";
    public static final String SEARCH = "M15.5 14h-.8l-.3-.3A6.5 6.5 0 1 0 14 15.5l.3.3v.8l5 5 1.5-1.5-5-5zm-6 0"
            + "A4.5 4.5 0 1 1 14 9.5 4.5 4.5 0 0 1 9.5 14z";
    public static final String CHAT = "M4 3h16a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H8l-5 4V5a2 2 0 0 1 2-2z";
    public static final String SPARK = "M12 2l2.2 6.8L21 11l-6.8 2.2L12 20l-2.2-6.8L3 11l6.8-2.2z";
    public static final String CHECK = "M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z";
    public static final String DOC = "M6 2h8l6 6v14H4V2h2zm7 1.5V9h5.5L13 3.5z";
    public static final String ALERT = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";

    private Components() {
    }

    public static Region icon(String svgPath, double size) {
        Region icon = new Region();
        icon.getStyleClass().add("icon");
        icon.setStyle("-fx-shape: \"" + svgPath + "\";");
        icon.setMinSize(size, size);
        icon.setPrefSize(size, size);
        icon.setMaxSize(size, size);
        return icon;
    }

    public static Button iconButton(String svgPath, String tooltip) {
        Button button = new Button(null, icon(svgPath, 15));
        button.getStyleClass().add("icon-button");
        button.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return button;
    }

    public static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    /** Logotipo da Lumi (o mesmo da animação GSAP): um "L" formado por pontos ligados. */
    public static StackPane logo(double size) {
        double s = size / 120.0;
        Rectangle background = new Rectangle(112 * s, 112 * s);
        background.setArcWidth(60 * s);
        background.setArcHeight(60 * s);
        background.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7c5cff")), new Stop(1, Color.web("#22d3ee"))));
        Polyline letter = new Polyline(40 * s, 30 * s, 40 * s, 86 * s, 84 * s, 86 * s);
        letter.setStroke(Color.WHITE);
        letter.setStrokeWidth(10 * s);
        letter.setStrokeLineCap(StrokeLineCap.ROUND);
        letter.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Group mark = new Group(letter,
                new Circle(40 * s, 30 * s, 9.5 * s, Color.WHITE),
                new Circle(40 * s, 86 * s, 9.5 * s, Color.WHITE),
                new Circle(84 * s, 86 * s, 9.5 * s, Color.WHITE),
                new Circle(82 * s, 38 * s, 7 * s, Color.web("#fff6b3")));
        StackPane logo = new StackPane(background, mark);
        logo.setMinSize(size, size);
        logo.setMaxSize(size, size);
        return logo;
    }

    /** Avatar redondo com a inicial do nome. */
    public static StackPane avatar(String name, double size) {
        Label letter = label(name == null || name.isBlank() ? "?" : name.trim().substring(0, 1).toUpperCase(),
                "avatar-letter");
        StackPane avatar = new StackPane(letter);
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(size, size);
        avatar.setMaxSize(size, size);
        return avatar;
    }

    /** Campo de formulário: rótulo + entrada + mensagem de erro do próprio campo. */
    public static class Field extends VBox {

        private final TextInputControl input;
        private final Label error = new Label();

        public Field(String label, String prompt, boolean password) {
            super(6);
            input = password ? new PasswordField() : new TextField();
            input.setPromptText(prompt);
            input.getStyleClass().add("form-input");
            input.textProperty().addListener((obs, old, value) -> clearError());
            error.getStyleClass().add("field-error");
            error.setWrapText(true);
            error.managedProperty().bind(error.visibleProperty());
            error.setVisible(false);
            getChildren().addAll(Components.label(label, "form-label"), input, error);
        }

        public TextInputControl input() { return input; }
        public String value() { return input.getText(); }

        public void showError(String message) {
            error.setText(message);
            error.setVisible(true);
            if (!input.getStyleClass().contains("invalid")) input.getStyleClass().add("invalid");
        }

        public void clearError() {
            error.setVisible(false);
            input.getStyleClass().remove("invalid");
        }
    }

    /** Faixa de mensagem de erro, sucesso ou informação. */
    public static class Banner extends HBox {

        private final Label text = new Label();
        private final Region icon = icon(ALERT, 14);

        public Banner() {
            super(10);
            getStyleClass().add("banner");
            text.setWrapText(true);
            text.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(text, Priority.ALWAYS);
            getChildren().addAll(icon, text);
            managedProperty().bind(visibleProperty());
            setVisible(false);
        }

        public void error(String message) { show("banner-error", ALERT, message); }
        public void success(String message) { show("banner-success", CHECK, message); }
        public void info(String message) { show("banner-info", ALERT, message); }
        public void hide() { setVisible(false); }

        private void show(String styleClass, String iconPath, String message) {
            getStyleClass().removeAll("banner-error", "banner-success", "banner-info");
            getStyleClass().add(styleClass);
            icon.setStyle("-fx-shape: \"" + iconPath + "\";");
            text.setText(message);
            setVisible(true);
            FadeTransition fade = new FadeTransition(Duration.millis(180), this);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }

    /** Botão principal que mostra um indicador de progresso durante a operação. */
    public static class LoadingButton extends Button {

        private final String idleText;
        private final ProgressIndicator spinner = new ProgressIndicator();

        public LoadingButton(String text) {
            super(text);
            idleText = text;
            spinner.getStyleClass().add("button-spinner");
            spinner.setMaxSize(16, 16);
            getStyleClass().add("primary-button");
            setMaxWidth(Double.MAX_VALUE);
        }

        public void setLoading(boolean loading, String loadingText) {
            setDisable(loading);
            setGraphic(loading ? spinner : null);
            setText(loading ? loadingText : idleText);
        }
    }

    /** "Lumi está pensando…" com três pontos animados (Timeline do JavaFX). */
    public static class TypingIndicator extends HBox {

        private final Timeline timeline = new Timeline();

        public TypingIndicator() {
            super(5);
            getStyleClass().add("typing-indicator");
            setAlignment(Pos.CENTER_LEFT);
            for (int i = 0; i < 3; i++) {
                Circle dot = new Circle(4);
                dot.getStyleClass().add("typing-dot");
                getChildren().add(dot);
                Duration start = Duration.millis(i * 160);
                timeline.getKeyFrames().addAll(
                        new KeyFrame(start, new KeyValue(dot.opacityProperty(), 0.3), new KeyValue(dot.translateYProperty(), 0)),
                        new KeyFrame(start.add(Duration.millis(300)), new KeyValue(dot.opacityProperty(), 1),
                                new KeyValue(dot.translateYProperty(), -4)),
                        new KeyFrame(start.add(Duration.millis(600)), new KeyValue(dot.opacityProperty(), 0.3),
                                new KeyValue(dot.translateYProperty(), 0)));
            }
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(1100)));
            timeline.setCycleCount(Animation.INDEFINITE);
            getChildren().add(label("A Lumi está consultando a base de conhecimento…", "typing-label"));
            managedProperty().bind(visibleProperty());
            setVisible(false);
        }

        public void setActive(boolean active) {
            setVisible(active);
            if (active) timeline.play(); else timeline.stop();
        }
    }
}
