package br.com.projeto.view.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderizador de Markdown simplificado para as respostas da IA, apenas com nós JavaFX:
 * títulos (#), listas (-, *, 1.), blocos de código (```), **negrito**, *itálico* e `código`.
 */
public final class MarkdownRenderer {

    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\*([^*\\s][^*]*?)\\*");
    private static final Pattern ORDERED = Pattern.compile("^(\\d+)[.)]\\s+(.*)");
    private static final Pattern BULLET = Pattern.compile("^[-*•]\\s+(.*)");
    private static final Pattern HEADING = Pattern.compile("^(#{1,4})\\s+(.*)");

    private MarkdownRenderer() {
    }

    public static VBox render(String markdown) {
        VBox root = new VBox(8);
        root.getStyleClass().add("markdown");
        String[] lines = markdown.replace("\r\n", "\n").split("\n", -1);

        List<String> paragraph = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.startsWith("```")) {
                flushParagraph(root, paragraph);
                String language = trimmed.substring(3).trim();
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("```")) {
                    code.append(lines[i]).append('\n');
                    i++;
                }
                root.getChildren().add(codeBlock(language, code.toString().stripTrailing()));
                continue;
            }

            Matcher heading = HEADING.matcher(trimmed);
            Matcher bullet = BULLET.matcher(trimmed);
            Matcher ordered = ORDERED.matcher(trimmed);
            if (trimmed.isEmpty()) {
                flushParagraph(root, paragraph);
            } else if (heading.matches()) {
                flushParagraph(root, paragraph);
                TextFlow flow = inline(heading.group(2));
                flow.getStyleClass().add("md-h" + Math.min(heading.group(1).length(), 3));
                root.getChildren().add(flow);
            } else if (bullet.matches()) {
                flushParagraph(root, paragraph);
                root.getChildren().add(listItem("•", bullet.group(1), indentOf(line)));
            } else if (ordered.matches()) {
                flushParagraph(root, paragraph);
                root.getChildren().add(listItem(ordered.group(1) + ".", ordered.group(2), indentOf(line)));
            } else if (trimmed.matches("^-{3,}$")) {
                flushParagraph(root, paragraph);
                Region rule = new Region();
                rule.getStyleClass().add("md-rule");
                root.getChildren().add(rule);
            } else {
                paragraph.add(trimmed.startsWith(">") ? trimmed.substring(1).trim() : trimmed);
            }
        }
        flushParagraph(root, paragraph);
        return root;
    }

    private static int indentOf(String line) {
        int spaces = 0;
        while (spaces < line.length() && line.charAt(spaces) == ' ') {
            spaces++;
        }
        return Math.min(spaces / 2, 3);
    }

    private static void flushParagraph(VBox root, List<String> paragraph) {
        if (paragraph.isEmpty()) {
            return;
        }
        TextFlow flow = inline(String.join(" ", paragraph));
        flow.getStyleClass().add("md-paragraph");
        root.getChildren().add(flow);
        paragraph.clear();
    }

    private static Node listItem(String marker, String text, int level) {
        Label bullet = new Label(marker);
        bullet.getStyleClass().add("md-bullet");
        bullet.setMinWidth(Region.USE_PREF_SIZE);
        TextFlow flow = inline(text);
        HBox.setHgrow(flow, Priority.ALWAYS);
        HBox item = new HBox(8, bullet, flow);
        item.setAlignment(Pos.TOP_LEFT);
        item.setStyle("-fx-padding: 0 0 0 " + (6 + level * 16) + ";");
        return item;
    }

    /** Converte formatação inline em nós Text com classes CSS. */
    static TextFlow inline(String text) {
        TextFlow flow = new TextFlow();
        Matcher m = INLINE.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                flow.getChildren().add(text(text.substring(last, m.start()), "md-text"));
            }
            if (m.group(1) != null) {
                flow.getChildren().add(text(m.group(1), "md-bold"));
            } else if (m.group(2) != null) {
                flow.getChildren().add(text(" " + m.group(2) + " ", "md-code-inline"));
            } else {
                flow.getChildren().add(text(m.group(3), "md-italic"));
            }
            last = m.end();
        }
        if (last < text.length()) {
            flow.getChildren().add(text(text.substring(last), "md-text"));
        }
        return flow;
    }

    private static Text text(String value, String styleClass) {
        Text t = new Text(value);
        t.getStyleClass().addAll("md-text", styleClass);
        return t;
    }

    private static Node codeBlock(String language, String code) {
        Label lang = new Label(language.isEmpty() ? "código" : language);
        lang.getStyleClass().add("code-lang");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button copy = new Button("Copiar", Icons.create(Icons.COPY, 12));
        copy.getStyleClass().add("code-copy");
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(code);
            Clipboard.getSystemClipboard().setContent(content);
            copy.setText("Copiado!");
        });
        HBox header = new HBox(8, lang, spacer, copy);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("code-header");

        Label body = new Label(code);
        body.getStyleClass().add("code-body");
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);

        VBox block = new VBox(header, body);
        block.getStyleClass().add("code-block");
        return block;
    }
}
