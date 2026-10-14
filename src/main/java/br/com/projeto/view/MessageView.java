package br.com.projeto.view;

import br.com.projeto.model.Message;
import br.com.projeto.util.Ui;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uma mensagem do chat: avatar + balão (usuário à direita, Lumi à esquerda).
 * As respostas da IA são renderizadas a partir de Markdown simples, só com nós JavaFX:
 * títulos (#), listas (-, 1.), blocos de código (```), **negrito**, *itálico* e `código`.
 */
public class MessageView extends HBox {

    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\*([^*\\s][^*]*?)\\*");
    private static final Pattern HEADING = Pattern.compile("^(#{1,4})\\s+(.*)");
    private static final Pattern BULLET = Pattern.compile("^[-*•]\\s+(.*)");
    private static final Pattern ORDERED = Pattern.compile("^(\\d+)[.)]\\s+(.*)");

    public MessageView(Message message, String userName, ObservableDoubleValue availableWidth) {
        boolean fromUser = message.getRole() == Message.Role.USER;
        getStyleClass().addAll("message-row", fromUser ? "from-user" : "from-assistant");
        setSpacing(12);
        setAlignment(fromUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

        HBox meta = new HBox(8, Components.label(fromUser ? "Você" : "Lumi", "message-author"),
                Components.label(Ui.formatDate(message.getCreatedAt()), "message-time"));
        meta.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Node content;
        if (fromUser) {
            Label text = Components.label(message.getContent(), "message-text");
            text.setWrapText(true);
            content = text;
        } else {
            content = renderMarkdown(message.getContent());
        }
        VBox bubble = new VBox(8, content);
        bubble.getStyleClass().add("bubble");
        bubble.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(260, Math.min(760, availableWidth.get() * (fromUser ? 0.7 : 0.82))), availableWidth));
        if (!fromUser) {
            bubble.getChildren().add(footer(message));
        }

        VBox column = new VBox(4, meta, bubble);
        column.setAlignment(fromUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        column.setFillWidth(!fromUser); // balão do usuário fica do tamanho do texto
        HBox.setHgrow(column, Priority.SOMETIMES);
        if (fromUser) {
            getChildren().addAll(column, Components.avatar(userName, 32));
        } else {
            getChildren().addAll(Components.logo(32), column);
        }
    }

    /** Fontes usadas pelo RAG + botão copiar. */
    private static Node footer(Message message) {
        FlowPane sources = new FlowPane(6, 6);
        HBox.setHgrow(sources, Priority.ALWAYS);
        if (message.getSources().isEmpty()) {
            sources.getChildren().add(Components.label("Resposta sem documentos da base", "sources-label"));
        } else {
            Label label = Components.label("Fontes:", "sources-label");
            label.setGraphic(Components.icon(Components.BOOK, 11));
            sources.getChildren().add(label);
            message.getSources().forEach(title -> sources.getChildren().add(Components.label(title, "source-chip")));
        }
        Button copy = new Button(null, Components.icon(Components.COPY, 13));
        copy.getStyleClass().add("icon-button");
        copy.setTooltip(new Tooltip("Copiar resposta"));
        copy.setOnAction(e -> {
            copyToClipboard(message.getContent());
            copy.setGraphic(Components.icon(Components.CHECK, 13));
        });
        HBox footer = new HBox(8, sources, copy);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("bubble-footer");
        return footer;
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

    // ---------- Markdown ----------

    static VBox renderMarkdown(String markdown) {
        VBox root = new VBox(8);
        String[] lines = markdown.replace("\r\n", "\n").split("\n", -1);
        List<String> paragraph = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                flush(root, paragraph);
                StringBuilder code = new StringBuilder();
                for (i++; i < lines.length && !lines[i].trim().startsWith("```"); i++) {
                    code.append(lines[i]).append('\n');
                }
                root.getChildren().add(codeBlock(trimmed.substring(3).trim(), code.toString().stripTrailing()));
                continue;
            }
            Matcher heading = HEADING.matcher(trimmed);
            Matcher bullet = BULLET.matcher(trimmed);
            Matcher ordered = ORDERED.matcher(trimmed);
            if (trimmed.isEmpty()) {
                flush(root, paragraph);
            } else if (heading.matches()) {
                flush(root, paragraph);
                TextFlow flow = inline(heading.group(2));
                flow.getStyleClass().add("md-h" + Math.min(heading.group(1).length(), 3));
                root.getChildren().add(flow);
            } else if (bullet.matches() || ordered.matches()) {
                flush(root, paragraph);
                String marker = bullet.matches() ? "•" : ordered.group(1) + ".";
                TextFlow flow = inline(bullet.matches() ? bullet.group(1) : ordered.group(2));
                HBox.setHgrow(flow, Priority.ALWAYS);
                Label markerLabel = Components.label(marker, "md-bullet");
                markerLabel.setMinWidth(Region.USE_PREF_SIZE);
                HBox item = new HBox(8, markerLabel, flow);
                int indent = (line.length() - line.stripLeading().length()) / 2;
                item.setStyle("-fx-padding: 0 0 0 " + (6 + Math.min(indent, 3) * 16) + ";");
                root.getChildren().add(item);
            } else {
                paragraph.add(trimmed.startsWith(">") ? trimmed.substring(1).trim() : trimmed);
            }
        }
        flush(root, paragraph);
        return root;
    }

    private static void flush(VBox root, List<String> paragraph) {
        if (!paragraph.isEmpty()) {
            TextFlow flow = inline(String.join(" ", paragraph));
            flow.getStyleClass().add("md-paragraph");
            root.getChildren().add(flow);
            paragraph.clear();
        }
    }

    private static TextFlow inline(String text) {
        TextFlow flow = new TextFlow();
        Matcher m = INLINE.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) flow.getChildren().add(text(text.substring(last, m.start()), null));
            if (m.group(1) != null) flow.getChildren().add(text(m.group(1), "md-bold"));
            else if (m.group(2) != null) flow.getChildren().add(text(" " + m.group(2) + " ", "md-code-inline"));
            else flow.getChildren().add(text(m.group(3), "md-italic"));
            last = m.end();
        }
        if (last < text.length()) flow.getChildren().add(text(text.substring(last), null));
        return flow;
    }

    private static Text text(String value, String styleClass) {
        Text t = new Text(value);
        t.getStyleClass().add("md-text");
        if (styleClass != null) t.getStyleClass().add(styleClass);
        return t;
    }

    private static Node codeBlock(String language, String code) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button copy = new Button("Copiar", Components.icon(Components.COPY, 12));
        copy.getStyleClass().add("code-copy");
        copy.setOnAction(e -> {
            copyToClipboard(code);
            copy.setText("Copiado!");
        });
        HBox header = new HBox(8, Components.label(language.isEmpty() ? "código" : language, "code-lang"), spacer, copy);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("code-header");
        Label body = Components.label(code, "code-body");
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);
        VBox block = new VBox(header, body);
        block.getStyleClass().add("code-block");
        return block;
    }

    private static void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
