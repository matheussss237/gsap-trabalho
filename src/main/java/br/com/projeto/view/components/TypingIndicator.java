package br.com.projeto.view.components;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/** Indicador "Nexus está pensando…" com três pontos animados. */
public class TypingIndicator extends HBox {

    private final Timeline timeline = new Timeline();

    public TypingIndicator() {
        getStyleClass().add("typing-indicator");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(5);
        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.getStyleClass().add("typing-dot");
            getChildren().add(dot);
            Duration offset = Duration.millis(i * 160);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(offset, new KeyValue(dot.opacityProperty(), 0.3), new KeyValue(dot.translateYProperty(), 0)),
                    new KeyFrame(offset.add(Duration.millis(300)), new KeyValue(dot.opacityProperty(), 1),
                            new KeyValue(dot.translateYProperty(), -4)),
                    new KeyFrame(offset.add(Duration.millis(600)), new KeyValue(dot.opacityProperty(), 0.3),
                            new KeyValue(dot.translateYProperty(), 0)));
        }
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(1100)));
        timeline.setCycleCount(Animation.INDEFINITE);

        Label label = new Label("Consultando a base de conhecimento e a IA…");
        label.getStyleClass().add("typing-label");
        getChildren().add(label);
        setSpacing(6);
    }

    public void start() {
        timeline.play();
    }

    public void stop() {
        timeline.stop();
    }
}
