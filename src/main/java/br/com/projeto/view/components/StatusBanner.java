package br.com.projeto.view.components;

import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/** Faixa de mensagem (erro, sucesso ou informação) exibida nos formulários e no chat. */
public class StatusBanner extends HBox {

    public enum Kind { ERROR, SUCCESS, INFO }

    private final Label text = new Label();
    private final Region icon = Icons.create(Icons.ALERT, 14);

    public StatusBanner() {
        getStyleClass().add("banner");
        setSpacing(10);
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);
        getChildren().addAll(icon, text);
        hide();
    }

    public void show(Kind kind, String message) {
        getStyleClass().removeAll("banner-error", "banner-success", "banner-info");
        getStyleClass().add("banner-" + kind.name().toLowerCase());
        icon.setStyle("-fx-shape: \"" + (kind == Kind.SUCCESS ? Icons.CHECK : Icons.ALERT) + "\";");
        text.setText(message);
        setManaged(true);
        setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.millis(180), this);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public void error(String message) {
        show(Kind.ERROR, message);
    }

    public void success(String message) {
        show(Kind.SUCCESS, message);
    }

    public void info(String message) {
        show(Kind.INFO, message);
    }

    public void hide() {
        setManaged(false);
        setVisible(false);
    }
}
