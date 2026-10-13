package br.com.projeto.view.components;

import br.com.projeto.model.Message;
import br.com.projeto.model.MessageRole;
import br.com.projeto.util.DateFormat;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Linha do chat contendo avatar + balão da mensagem (usuário à direita, IA à esquerda). */
public class MessageBubble extends HBox {

    public MessageBubble(Message message, String userName, ObservableDoubleValue availableWidth) {
        boolean fromUser = message.getRole() == MessageRole.USER;
        getStyleClass().addAll("message-row", fromUser ? "from-user" : "from-assistant");
        setSpacing(12);
        setAlignment(fromUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

        Label author = new Label(fromUser ? "Você" : "Nexus");
        author.getStyleClass().add("message-author");
        Label time = new Label(DateFormat.smart(message.getCreatedAt()));
        time.getStyleClass().add("message-time");
        HBox meta = new HBox(8, author, time);
        meta.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Node content;
        if (fromUser) {
            Label text = new Label(message.getContent());
            text.setWrapText(true);
            text.getStyleClass().add("message-text");
            content = text;
        } else {
            content = MarkdownRenderer.render(message.getContent());
        }

        VBox bubble = new VBox(8, content);
        bubble.getStyleClass().add("bubble");
        bubble.maxWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.max(260, Math.min(760, availableWidth.get() * (fromUser ? 0.7 : 0.82))), availableWidth));

        if (!fromUser) {
            bubble.getChildren().add(footer(message));
        }

        VBox column = new VBox(4, meta, bubble);
        column.setAlignment(fromUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        column.setFillWidth(!fromUser); // balão do usuário fica do tamanho do texto
        HBox.setHgrow(column, Priority.SOMETIMES);

        Node avatar = fromUser ? userAvatar(userName) : LogoMark.create(32);
        if (fromUser) {
            getChildren().addAll(column, avatar);
        } else {
            getChildren().addAll(avatar, column);
        }
    }

    /** Fontes do RAG + botão copiar. */
    private Node footer(Message message) {
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("bubble-footer");

        FlowPane sources = new FlowPane(6, 6);
        HBox.setHgrow(sources, Priority.ALWAYS);
        if (!message.getSources().isEmpty()) {
            Label label = new Label("Fontes:", Icons.create(Icons.BOOK, 11));
            label.getStyleClass().add("sources-label");
            sources.getChildren().add(label);
            for (String title : message.getSources()) {
                Label chip = new Label(title);
                chip.getStyleClass().add("source-chip");
                sources.getChildren().add(chip);
            }
        } else {
            Label none = new Label("Resposta sem documentos da base");
            none.getStyleClass().add("sources-label");
            sources.getChildren().add(none);
        }

        Button copy = new Button(null, Icons.create(Icons.COPY, 13));
        copy.getStyleClass().add("icon-button");
        copy.setTooltip(new Tooltip("Copiar resposta"));
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(message.getContent());
            Clipboard.getSystemClipboard().setContent(content);
            copy.setGraphic(Icons.create(Icons.CHECK, 13));
        });
        footer.getChildren().addAll(sources, copy);
        return footer;
    }

    private static Node userAvatar(String name) {
        String initial = name == null || name.isBlank() ? "?" : name.trim().substring(0, 1).toUpperCase();
        Label letter = new Label(initial);
        letter.getStyleClass().add("avatar-letter");
        StackPane avatar = new StackPane(letter);
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        return avatar;
    }

    public void animateIn() {
        setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(220), this);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), this);
        slide.setFromY(12);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    public void markPending(boolean pending) {
        setOpacity(pending ? 0.75 : 1);
    }
}
