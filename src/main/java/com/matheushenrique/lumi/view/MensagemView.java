package com.matheushenrique.lumi.view;

import com.matheushenrique.lumi.infrastructure.entitys.Mensagem;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Um balão do chat (usuário na direita, Lumi na esquerda).
 * A resposta da IA vem em Markdown; aqui ela vira componentes JavaFX:
 * títulos (#), listas (-, 1.), código (```), **negrito**, *itálico* e `código`.
 */
public class MensagemView extends HBox {

    private static final Pattern FORMATACAO = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\*([^*\\s][^*]*?)\\*");

    public MensagemView(Mensagem mensagem, String nomeUsuario, ObservableDoubleValue larguraDisponivel) {
        boolean doUsuario = mensagem.getAutor() == Mensagem.Autor.USUARIO;
        getStyleClass().addAll("message-row", doUsuario ? "from-user" : "from-assistant");
        setSpacing(12);
        setAlignment(doUsuario ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

        HBox cabecalho = new HBox(8, Componentes.texto(doUsuario ? "Você" : "Lumi", "message-author"),
                Componentes.texto(Componentes.formatarData(mensagem.getCriadoEm()), "message-time"));
        cabecalho.setAlignment(doUsuario ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Node conteudo;
        if (doUsuario) {
            Label texto = Componentes.texto(mensagem.getConteudo(), "message-text");
            texto.setWrapText(true);
            conteudo = texto;
        } else {
            conteudo = markdown(mensagem.getConteudo());
        }
        VBox balao = new VBox(8, conteudo);
        balao.getStyleClass().add("bubble");
        balao.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(260, Math.min(760, larguraDisponivel.get() * (doUsuario ? 0.7 : 0.82))), larguraDisponivel));
        if (!doUsuario) {
            balao.getChildren().add(rodape(mensagem));
        }

        VBox coluna = new VBox(4, cabecalho, balao);
        coluna.setAlignment(doUsuario ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        coluna.setFillWidth(!doUsuario); // balão do usuário fica do tamanho do texto
        HBox.setHgrow(coluna, Priority.SOMETIMES);
        if (doUsuario) {
            getChildren().addAll(coluna, Componentes.avatar(nomeUsuario, 32));
        } else {
            getChildren().addAll(Componentes.logo(32), coluna);
        }
    }

    // fontes usadas pelo RAG + botão de copiar
    private static Node rodape(Mensagem mensagem) {
        FlowPane fontes = new FlowPane(6, 6);
        HBox.setHgrow(fontes, Priority.ALWAYS);
        if (mensagem.getFontes().isEmpty()) {
            fontes.getChildren().add(Componentes.texto("Resposta sem documentos da base", "sources-label"));
        } else {
            Label rotulo = Componentes.texto("Fontes:", "sources-label");
            rotulo.setGraphic(Componentes.icone(Componentes.LIVRO, 11));
            fontes.getChildren().add(rotulo);
            mensagem.getFontes().forEach(titulo -> fontes.getChildren().add(Componentes.texto(titulo, "source-chip")));
        }
        Button copiar = Componentes.botaoIcone(Componentes.COPIAR, "Copiar resposta");
        copiar.setOnAction(e -> {
            copiar(mensagem.getConteudo());
            copiar.setGraphic(Componentes.icone(Componentes.CERTO, 13));
        });
        HBox rodape = new HBox(8, fontes, copiar);
        rodape.setAlignment(Pos.CENTER_LEFT);
        rodape.getStyleClass().add("bubble-footer");
        return rodape;
    }

    public void animarEntrada() {
        setOpacity(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(220), this);
        aparecer.setToValue(1);
        TranslateTransition subir = new TranslateTransition(Duration.millis(220), this);
        subir.setFromY(12);
        subir.setToY(0);
        aparecer.play();
        subir.play();
    }

    // ---------- Markdown -> JavaFX ----------

    static VBox markdown(String texto) {
        VBox raiz = new VBox(8);
        String[] linhas = texto.replace("\r\n", "\n").split("\n", -1);
        List<String> paragrafo = new ArrayList<>();
        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i].trim();
            if (linha.startsWith("```")) {
                fecharParagrafo(raiz, paragrafo);
                StringBuilder codigo = new StringBuilder();
                for (i++; i < linhas.length && !linhas[i].trim().startsWith("```"); i++) {
                    codigo.append(linhas[i]).append('\n');
                }
                raiz.getChildren().add(blocoCodigo(linha.substring(3).trim(), codigo.toString().stripTrailing()));
            } else if (linha.isEmpty()) {
                fecharParagrafo(raiz, paragrafo);
            } else if (linha.matches("^#{1,4}\\s+.*")) {
                fecharParagrafo(raiz, paragrafo);
                int nivel = Math.min(linha.indexOf(' '), 3);
                TextFlow titulo = formatar(linha.substring(linha.indexOf(' ') + 1));
                titulo.getStyleClass().add("md-h" + nivel);
                raiz.getChildren().add(titulo);
            } else if (linha.matches("^([-*•]|\\d+[.)])\\s+.*")) {
                fecharParagrafo(raiz, paragrafo);
                String marcador = linha.matches("^\\d+.*") ? linha.substring(0, linha.indexOf(' ')) : "•";
                Label bolinha = Componentes.texto(marcador, "md-bullet");
                bolinha.setMinWidth(Region.USE_PREF_SIZE);
                TextFlow item = formatar(linha.substring(linha.indexOf(' ') + 1));
                HBox.setHgrow(item, Priority.ALWAYS);
                HBox linhaLista = new HBox(8, bolinha, item);
                int recuo = (linhas[i].length() - linhas[i].stripLeading().length()) / 2;
                linhaLista.setStyle("-fx-padding: 0 0 0 " + (6 + Math.min(recuo, 3) * 16) + ";");
                raiz.getChildren().add(linhaLista);
            } else {
                paragrafo.add(linha.startsWith(">") ? linha.substring(1).trim() : linha);
            }
        }
        fecharParagrafo(raiz, paragrafo);
        return raiz;
    }

    private static void fecharParagrafo(VBox raiz, List<String> paragrafo) {
        if (!paragrafo.isEmpty()) {
            TextFlow texto = formatar(String.join(" ", paragrafo));
            texto.getStyleClass().add("md-paragraph");
            raiz.getChildren().add(texto);
            paragrafo.clear();
        }
    }

    private static TextFlow formatar(String texto) {
        TextFlow fluxo = new TextFlow();
        Matcher m = FORMATACAO.matcher(texto);
        int ultimo = 0;
        while (m.find()) {
            if (m.start() > ultimo) fluxo.getChildren().add(pedaco(texto.substring(ultimo, m.start()), null));
            if (m.group(1) != null) fluxo.getChildren().add(pedaco(m.group(1), "md-bold"));
            else if (m.group(2) != null) fluxo.getChildren().add(pedaco(" " + m.group(2) + " ", "md-code-inline"));
            else fluxo.getChildren().add(pedaco(m.group(3), "md-italic"));
            ultimo = m.end();
        }
        if (ultimo < texto.length()) fluxo.getChildren().add(pedaco(texto.substring(ultimo), null));
        return fluxo;
    }

    private static Text pedaco(String valor, String estilo) {
        Text texto = new Text(valor);
        texto.getStyleClass().add("md-text");
        if (estilo != null) texto.getStyleClass().add(estilo);
        return texto;
    }

    private static Node blocoCodigo(String linguagem, String codigo) {
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        Button copiar = new Button("Copiar", Componentes.icone(Componentes.COPIAR, 12));
        copiar.getStyleClass().add("code-copy");
        copiar.setOnAction(e -> {
            copiar(codigo);
            copiar.setText("Copiado!");
        });
        HBox topo = new HBox(8, Componentes.texto(linguagem.isEmpty() ? "código" : linguagem, "code-lang"), espaco, copiar);
        topo.setAlignment(Pos.CENTER_LEFT);
        topo.getStyleClass().add("code-header");
        Label corpo = Componentes.texto(codigo, "code-body");
        corpo.setWrapText(true);
        corpo.setMaxWidth(Double.MAX_VALUE);
        VBox bloco = new VBox(topo, corpo);
        bloco.getStyleClass().add("code-block");
        return bloco;
    }

    private static void copiar(String texto) {
        ClipboardContent conteudo = new ClipboardContent();
        conteudo.putString(texto);
        Clipboard.getSystemClipboard().setContent(conteudo);
    }
}
