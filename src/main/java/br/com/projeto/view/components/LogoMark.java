package br.com.projeto.view.components;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/** Logotipo do Nexus Chat desenhado com formas JavaFX (o mesmo da animação GSAP). */
public final class LogoMark {

    private LogoMark() {
    }

    public static StackPane create(double size) {
        double s = size / 120.0;

        Rectangle background = new Rectangle(112 * s, 112 * s);
        background.setArcWidth(60 * s);
        background.setArcHeight(60 * s);
        background.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7c5cff")), new Stop(1, Color.web("#22d3ee"))));

        Polyline n = new Polyline(34 * s, 86 * s, 34 * s, 34 * s, 86 * s, 86 * s, 86 * s, 34 * s);
        n.setStroke(Color.WHITE);
        n.setStrokeWidth(9 * s);
        n.setStrokeLineCap(StrokeLineCap.ROUND);
        n.setStrokeLineJoin(StrokeLineJoin.ROUND);
        n.setFill(null);

        Group mark = new Group(n,
                dot(34 * s, 86 * s, 9 * s), dot(34 * s, 34 * s, 9 * s),
                dot(86 * s, 86 * s, 9 * s), dot(86 * s, 34 * s, 9 * s));

        StackPane logo = new StackPane(background, mark);
        logo.getStyleClass().add("logo-mark");
        logo.setMinSize(size, size);
        logo.setMaxSize(size, size);
        return logo;
    }

    private static Circle dot(double x, double y, double r) {
        Circle c = new Circle(x, y, r, Color.WHITE);
        return c;
    }
}
