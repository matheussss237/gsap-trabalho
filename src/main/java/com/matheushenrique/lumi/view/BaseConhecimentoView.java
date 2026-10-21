package com.matheushenrique.lumi.view;

import com.matheushenrique.lumi.infrastructure.entitys.Documento;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/*
 * Janela da base de conhecimento do RAG:
 * aba "Documentos" (cadastrar/ver/excluir) e aba "Testar busca" (mostra palavras-chave,
 * stop words removidas e a pontuação de cada documento).
 */
public class BaseConhecimentoView {

    // aba Documentos
    public final ListView<Documento> listaDocumentos = new ListView<>();
    public final Button botaoNovo = new Button("Novo documento", Componentes.icone(Componentes.MAIS, 12));
    public final Componentes.Campo titulo = new Componentes.Campo("Título", "Ex.: Política de férias", false);
    public final Componentes.Campo palavrasChave =
            new Componentes.Campo("Palavras-chave (separadas por vírgula)", "férias, rh, benefícios", false);
    public final TextArea conteudo = new TextArea();
    public final Componentes.BotaoCarregando botaoSalvar = new Componentes.BotaoCarregando("Salvar documento");
    public final Button botaoExcluir = new Button("Excluir", Componentes.icone(Componentes.LIXEIRA, 12));
    public final Componentes.Aviso aviso = new Componentes.Aviso();

    // aba Testar busca
    public final TextField campoPergunta = new TextField();
    public final Button botaoPesquisar = new Button("Pesquisar", Componentes.icone(Componentes.LUPA, 12));
    public final FlowPane palavrasEncontradas = new FlowPane(6, 6);
    public final FlowPane stopWordsRemovidas = new FlowPane(6, 6);
    public final VBox resultados = new VBox(10);

    public final Button botaoFechar = new Button("Fechar");

    private final Label tituloEditor = Componentes.texto("Novo documento", "section-heading");
    private final Label palavrasSalvas = Componentes.texto("", "muted");
    private final BorderPane raiz = new BorderPane();

    public BaseConhecimentoView() {
        raiz.getStyleClass().add("knowledge-root");
        TabPane abas = new TabPane(new Tab("Documentos", abaDocumentos()), new Tab("Testar busca (RAG)", abaTeste()));
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox topo = new VBox(2, Componentes.texto("Base de conhecimento", "dialog-title"),
                Componentes.texto("Documentos que o RAG usa para dar contexto às respostas da Lumi.", "muted"));
        topo.getStyleClass().add("dialog-header");
        botaoFechar.getStyleClass().add("secondary-button");
        botaoFechar.setCancelButton(true);
        HBox rodape = new HBox(botaoFechar);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        rodape.getStyleClass().add("dialog-footer");

        raiz.setTop(topo);
        raiz.setCenter(abas);
        raiz.setBottom(rodape);
        mostrarNovoDocumento();
    }

    private BorderPane abaDocumentos() {
        botaoNovo.getStyleClass().add("new-chat-button");
        botaoNovo.setMaxWidth(Double.MAX_VALUE);
        listaDocumentos.setCellFactory(lista -> new ListCell<>() {
            { setPrefWidth(0); }

            @Override
            protected void updateItem(Documento documento, boolean vazio) {
                super.updateItem(documento, vazio);
                setText(null);
                if (vazio || documento == null) {
                    setGraphic(null);
                    return;
                }
                Label nome = Componentes.texto(documento.getTitulo(), "conversation-title");
                nome.setMinWidth(0);
                Label etiqueta = Componentes.texto(documento.isPublico() ? "público" : "meu", "badge");
                etiqueta.getStyleClass().add(documento.isPublico() ? "badge-public" : "badge-mine");
                Region espaco = new Region();
                HBox.setHgrow(espaco, Priority.ALWAYS);
                HBox linha = new HBox(8, Componentes.icone(Componentes.DOCUMENTO, 13), nome, espaco, etiqueta);
                linha.setAlignment(Pos.CENTER_LEFT);
                setGraphic(linha);
            }
        });
        VBox.setVgrow(listaDocumentos, Priority.ALWAYS);
        VBox esquerda = new VBox(10, botaoNovo, listaDocumentos);
        esquerda.setPrefWidth(270);
        esquerda.setMinWidth(220);
        esquerda.getStyleClass().add("knowledge-list-pane");

        conteudo.setWrapText(true);
        conteudo.setPromptText("Texto que a IA pode usar como contexto…");
        conteudo.getStyleClass().add("form-input");
        VBox.setVgrow(conteudo, Priority.ALWAYS);
        VBox caixaConteudo = new VBox(6, Componentes.texto("Conteúdo", "form-label"), conteudo);
        VBox.setVgrow(caixaConteudo, Priority.ALWAYS);
        palavrasSalvas.setWrapText(true);
        palavrasSalvas.managedProperty().bind(palavrasSalvas.visibleProperty());
        palavrasChave.managedProperty().bind(palavrasChave.visibleProperty());
        botaoSalvar.setMaxWidth(220);
        botaoSalvar.managedProperty().bind(botaoSalvar.visibleProperty());
        botaoExcluir.getStyleClass().add("danger-button");
        botaoExcluir.managedProperty().bind(botaoExcluir.visibleProperty());
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        VBox editor = new VBox(12, tituloEditor, aviso, titulo, palavrasChave, caixaConteudo, palavrasSalvas,
                new HBox(10, botaoSalvar, espaco, botaoExcluir));
        editor.setPadding(new Insets(4, 4, 4, 18));
        BorderPane aba = new BorderPane(editor);
        aba.setLeft(esquerda);
        aba.setPadding(new Insets(16, 24, 8, 24));
        return aba;
    }

    private ScrollPane abaTeste() {
        Label explicacao = Componentes.texto("Digite uma pergunta como no chat. Aqui aparece cada etapa do RAG: "
                + "stop words removidas, palavras-chave e os documentos encontrados com a pontuação.", "muted");
        explicacao.setWrapText(true);
        campoPergunta.setPromptText("Ex.: Qual é o horário do suporte técnico da empresa?");
        HBox.setHgrow(campoPergunta, Priority.ALWAYS);
        botaoPesquisar.getStyleClass().add("primary-button");
        HBox linhaBusca = new HBox(10, campoPergunta, botaoPesquisar);
        linhaBusca.setAlignment(Pos.CENTER_LEFT);
        VBox caixa = new VBox(14, explicacao, linhaBusca,
                Componentes.texto("Palavras-chave extraídas", "form-label"), palavrasEncontradas,
                Componentes.texto("Stop words removidas", "form-label"), stopWordsRemovidas,
                Componentes.texto("Documentos encontrados", "form-label"), resultados);
        caixa.setPadding(new Insets(20, 24, 20, 24));
        ScrollPane rolagem = new ScrollPane(caixa);
        rolagem.setFitToWidth(true);
        rolagem.getStyleClass().add("transparent-scroll");
        return rolagem;
    }

    // mostra um documento já salvo (só leitura; "Excluir" aparece só se for seu)
    public void mostrarDocumento(Documento documento, boolean meu) {
        tituloEditor.setText(documento.isPublico() ? "Documento público (exemplo)" : "Seu documento");
        titulo.getEntrada().setText(documento.getTitulo());
        conteudo.setText(documento.getConteudo());
        palavrasSalvas.setText("Palavras-chave salvas: " + String.join(", ", documento.getPalavrasChave()));
        modoEdicao(false);
        botaoExcluir.setVisible(meu);
        aviso.esconder();
    }

    public void mostrarNovoDocumento() {
        tituloEditor.setText("Novo documento");
        titulo.getEntrada().clear();
        palavrasChave.getEntrada().clear();
        conteudo.clear();
        modoEdicao(true);
        botaoExcluir.setVisible(false);
    }

    private void modoEdicao(boolean editando) {
        titulo.getEntrada().setEditable(editando);
        conteudo.setEditable(editando);
        palavrasChave.setVisible(editando);
        botaoSalvar.setVisible(editando);
        palavrasSalvas.setVisible(!editando);
    }

    public static Label etiqueta(String texto, String estilo) {
        Label etiqueta = Componentes.texto(texto, "keyword-chip");
        etiqueta.getStyleClass().add(estilo);
        return etiqueta;
    }

    public Parent getRaiz() {
        return raiz;
    }
}
