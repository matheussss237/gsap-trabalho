package com.matheushenrique.lumi.view;

import com.matheushenrique.lumi.business.ChatService;
import com.matheushenrique.lumi.infrastructure.entitys.Conversa;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

/*
 * Tela principal: barra lateral (nova conversa, busca, lista, base de conhecimento, usuário,
 * tema e sair) + área do chat (título, mensagens e campo para digitar).
 */
public class ChatView {

    public static final List<String> SUGESTOES = List.of(
            "Qual é a política de home office da empresa?",
            "Explique encapsulamento em Java",
            "Como funciona o RAG?",
            "Como abrir um chamado no suporte técnico?");

    // barra lateral
    public final VBox barraLateral = new VBox(12);
    public final Button botaoNovaConversa = new Button("Nova conversa", Componentes.icone(Componentes.MAIS, 14));
    public final TextField campoBusca = new TextField();
    public final ListView<Conversa> listaConversas = new ListView<>();
    public final Button botaoBaseConhecimento = new Button("Base de conhecimento", Componentes.icone(Componentes.LIVRO, 14));
    public final Button botaoTema = Componentes.botaoIcone(Componentes.SOL, "Tema claro/escuro");
    public final Button botaoSair = Componentes.botaoIcone(Componentes.SAIR, "Sair da conta");

    // chat
    public final Button botaoMenu = Componentes.botaoIcone(Componentes.MENU, "Mostrar/esconder conversas");
    public final Label tituloConversa = Componentes.texto("Nova conversa", "chat-title");
    public final Label dataConversa = Componentes.texto("", "chat-subtitle");
    public final Button botaoRenomear = Componentes.botaoIcone(Componentes.LAPIS, "Renomear conversa");
    public final Button botaoExcluir = Componentes.botaoIcone(Componentes.LIXEIRA, "Excluir conversa");
    public final VBox mensagens = new VBox(22);
    public final ScrollPane rolagemMensagens = new ScrollPane(mensagens);
    public final Label saudacao = Componentes.texto("", "empty-title");
    public final FlowPane sugestoes = new FlowPane(10, 10);
    public final Componentes.Digitando digitando = new Componentes.Digitando();
    public final Componentes.Aviso aviso = new Componentes.Aviso();
    public final TextArea campoMensagem = new TextArea();
    public final Button botaoEnviar = new Button(null, Componentes.icone(Componentes.ENVIAR, 16));
    public final Label contador = Componentes.texto("", "composer-hint");

    // o que fazer ao clicar em renomear/excluir de uma conversa da lista (o controller define)
    public Consumer<Conversa> aoRenomear = c -> { };
    public Consumer<Conversa> aoExcluir = c -> { };

    private final BorderPane raiz = new BorderPane();
    private final VBox telaVazia;
    private final StackPane carregando;

    public ChatView(String nomeUsuario, String emailUsuario) {
        raiz.getStyleClass().add("chat-root");
        montarBarraLateral(nomeUsuario, emailUsuario);

        // topo do chat
        VBox titulos = new VBox(1, tituloConversa, dataConversa);
        titulos.setMinWidth(0);
        HBox.setHgrow(titulos, Priority.ALWAYS);
        botaoExcluir.getStyleClass().add("danger-action");
        HBox topo = new HBox(12, botaoMenu, titulos, botaoRenomear, botaoExcluir);
        topo.setAlignment(Pos.CENTER_LEFT);
        topo.getStyleClass().add("chat-header");

        // mensagens
        mensagens.setPadding(new Insets(28, 32, 28, 32));
        rolagemMensagens.setFitToWidth(true);
        rolagemMensagens.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rolagemMensagens.getStyleClass().add("transparent-scroll");
        mensagens.heightProperty().addListener((obs, antigo, novo) -> rolagemMensagens.setVvalue(1.0)); // desce até o fim

        Label explicacao = Componentes.texto("Pergunte qualquer coisa. Quando a pergunta tiver a ver com a base de "
                + "conhecimento, a Lumi usa esses documentos e mostra as fontes.", "empty-subtitle");
        explicacao.setWrapText(true);
        explicacao.setMaxWidth(520);
        explicacao.setStyle("-fx-text-alignment: center;");
        sugestoes.setAlignment(Pos.CENTER);
        sugestoes.setMaxWidth(640);
        telaVazia = new VBox(16, Componentes.logo(72), saudacao, explicacao, sugestoes);
        telaVazia.setAlignment(Pos.CENTER);
        telaVazia.setPadding(new Insets(24));

        ProgressIndicator girando = new ProgressIndicator();
        girando.setMaxSize(36, 36);
        VBox caixaCarregando = new VBox(10, girando, Componentes.texto("Carregando conversa…", "muted"));
        caixaCarregando.setAlignment(Pos.CENTER);
        carregando = new StackPane(caixaCarregando);
        carregando.getStyleClass().add("loading-overlay");
        carregando.setVisible(false);
        StackPane areaChat = new StackPane(rolagemMensagens, telaVazia, carregando);
        VBox.setVgrow(areaChat, Priority.ALWAYS);

        // campo de digitar
        campoMensagem.setPromptText("Pergunte qualquer coisa à Lumi…  (Enter envia, Shift+Enter pula linha)");
        campoMensagem.setWrapText(true);
        campoMensagem.setPrefRowCount(2);
        campoMensagem.getStyleClass().add("composer-input");
        HBox.setHgrow(campoMensagem, Priority.ALWAYS);
        botaoEnviar.getStyleClass().add("send-button");
        HBox caixaDigitar = new HBox(10, campoMensagem, botaoEnviar);
        caixaDigitar.setAlignment(Pos.BOTTOM_RIGHT);
        caixaDigitar.getStyleClass().add("composer");
        Label dicaRag = Componentes.texto("RAG ativo · a Lumi usa a base de conhecimento quando a pergunta tem relação",
                "composer-hint");
        dicaRag.setGraphic(Componentes.icone(Componentes.LIVRO, 11));
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        campoMensagem.textProperty().addListener((obs, antigo, texto) -> atualizarContador(texto.length()));
        atualizarContador(0);
        VBox rodape = new VBox(8, digitando, aviso, caixaDigitar, new HBox(dicaRag, espaco, contador));
        rodape.getStyleClass().add("composer-area");

        barraLateral.managedProperty().bind(barraLateral.visibleProperty());
        raiz.setLeft(barraLateral);
        raiz.setCenter(new VBox(topo, areaChat, rodape));
        // janela estreita: a barra lateral se esconde sozinha (o botão ☰ mostra de novo)
        raiz.widthProperty().addListener((obs, antigo, largura) -> {
            if (largura.doubleValue() < 820 && antigo.doubleValue() >= 820) barraLateral.setVisible(false);
            if (largura.doubleValue() >= 820 && antigo.doubleValue() < 820) barraLateral.setVisible(true);
        });
    }

    private void montarBarraLateral(String nomeUsuario, String emailUsuario) {
        barraLateral.getStyleClass().add("sidebar");
        barraLateral.setPrefWidth(280);
        barraLateral.setMinWidth(240);

        HBox topo = new HBox(10, Componentes.logo(30), Componentes.texto("Lumi", "sidebar-title"));
        topo.setAlignment(Pos.CENTER_LEFT);
        topo.setPadding(new Insets(0, 4, 6, 4));

        botaoNovaConversa.getStyleClass().add("new-chat-button");
        botaoNovaConversa.setMaxWidth(Double.MAX_VALUE);
        campoBusca.setPromptText("Buscar conversas");
        campoBusca.getStyleClass().add("search-field");
        HBox.setHgrow(campoBusca, Priority.ALWAYS);
        HBox busca = new HBox(8, Componentes.icone(Componentes.LUPA, 14), campoBusca);
        busca.setAlignment(Pos.CENTER_LEFT);
        busca.getStyleClass().add("search-box");

        listaConversas.setCellFactory(lista -> new CelulaConversa());
        Label vazio = Componentes.texto("Nenhuma conversa ainda.\nClique em \"Nova conversa\" para começar.", "empty-list");
        vazio.setWrapText(true);
        listaConversas.setPlaceholder(vazio);
        VBox.setVgrow(listaConversas, Priority.ALWAYS);

        botaoBaseConhecimento.getStyleClass().add("sidebar-link");
        botaoBaseConhecimento.setMaxWidth(Double.MAX_VALUE);
        botaoBaseConhecimento.setAlignment(Pos.CENTER_LEFT);
        botaoSair.getStyleClass().add("logout-button");

        VBox dadosUsuario = new VBox(1, Componentes.texto(nomeUsuario, "user-name"), Componentes.texto(emailUsuario, "user-email"));
        dadosUsuario.setMinWidth(0);
        HBox.setHgrow(dadosUsuario, Priority.ALWAYS);
        HBox cartaoUsuario = new HBox(10, Componentes.avatar(nomeUsuario, 34), dadosUsuario, botaoTema, botaoSair);
        cartaoUsuario.setAlignment(Pos.CENTER_LEFT);
        cartaoUsuario.getStyleClass().add("user-card");

        barraLateral.getChildren().addAll(topo, botaoNovaConversa, busca, Componentes.texto("CONVERSAS", "section-title"),
                listaConversas, botaoBaseConhecimento, cartaoUsuario);
    }

    private void atualizarContador(int tamanho) {
        contador.setText(tamanho + "/" + ChatService.TAMANHO_MAXIMO);
        contador.getStyleClass().remove("over-limit");
        if (tamanho > ChatService.TAMANHO_MAXIMO) contador.getStyleClass().add("over-limit");
    }

    public void mostrarTelaVazia(boolean mostrar) {
        telaVazia.setVisible(mostrar);
        rolagemMensagens.setVisible(!mostrar);
    }

    public void mostrarCarregando(boolean mostrar) {
        carregando.setVisible(mostrar);
    }

    public void adicionarMensagem(Node mensagem) {
        mensagens.getChildren().add(mensagem);
        mostrarTelaVazia(false);
    }

    public Parent getRaiz() {
        return raiz;
    }

    // linha da lista de conversas: título, data e botões de renomear/excluir (também no clique direito)
    private class CelulaConversa extends ListCell<Conversa> {

        private final Label titulo = Componentes.texto("", "conversation-title");
        private final Label data = Componentes.texto("", "conversation-date");
        private final HBox conteudo;
        private final ContextMenu menu;

        CelulaConversa() {
            setPrefWidth(0); // acompanha a largura da lista
            Button renomear = Componentes.botaoIcone(Componentes.LAPIS, "Renomear");
            Button excluir = Componentes.botaoIcone(Componentes.LIXEIRA, "Excluir");
            renomear.getStyleClass().add("cell-action");
            excluir.getStyleClass().addAll("cell-action", "danger-action");
            HBox acoes = new HBox(2, renomear, excluir);
            acoes.visibleProperty().bind(hoverProperty().or(selectedProperty()));
            VBox textos = new VBox(2, titulo, data);
            textos.setMinWidth(0);
            HBox.setHgrow(textos, Priority.ALWAYS);
            conteudo = new HBox(10, Componentes.icone(Componentes.BALAO, 14), textos, acoes);
            conteudo.setAlignment(Pos.CENTER_LEFT);

            renomear.setOnAction(e -> aoRenomear.accept(getItem()));
            excluir.setOnAction(e -> aoExcluir.accept(getItem()));
            MenuItem itemRenomear = new MenuItem("Renomear");
            itemRenomear.setOnAction(e -> aoRenomear.accept(getItem()));
            MenuItem itemExcluir = new MenuItem("Excluir");
            itemExcluir.setOnAction(e -> aoExcluir.accept(getItem()));
            menu = new ContextMenu(itemRenomear, itemExcluir);
        }

        @Override
        protected void updateItem(Conversa conversa, boolean vazio) {
            super.updateItem(conversa, vazio);
            setText(null);
            if (vazio || conversa == null) {
                setGraphic(null);
                setContextMenu(null);
            } else {
                titulo.setText(conversa.getTitulo());
                data.setText(Componentes.formatarData(conversa.getAtualizadoEm()));
                setGraphic(conteudo);
                setContextMenu(menu);
            }
        }
    }
}
