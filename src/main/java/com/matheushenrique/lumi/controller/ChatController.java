package com.matheushenrique.lumi.controller;

import com.matheushenrique.lumi.Main;
import com.matheushenrique.lumi.business.ChatService;
import com.matheushenrique.lumi.business.ConversaService;
import com.matheushenrique.lumi.business.RagService;
import com.matheushenrique.lumi.business.UsuarioService;
import com.matheushenrique.lumi.dto.RespostaChatDTO;
import com.matheushenrique.lumi.dto.ResultadoRagDTO;
import com.matheushenrique.lumi.infrastructure.entitys.Conversa;
import com.matheushenrique.lumi.infrastructure.entitys.Documento;
import com.matheushenrique.lumi.infrastructure.entitys.Mensagem;
import com.matheushenrique.lumi.infrastructure.entitys.Usuario;
import com.matheushenrique.lumi.rag.ProcessadorTexto;
import com.matheushenrique.lumi.view.BaseConhecimentoView;
import com.matheushenrique.lumi.view.ChatView;
import com.matheushenrique.lumi.view.Componentes;
import com.matheushenrique.lumi.view.MensagemView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;

/*
 * Ações da tela principal:
 * - barra lateral: listar, criar, buscar, renomear e excluir conversas; sair
 * - chat: enviar mensagem (ChatService -> RagService -> GroqService)
 * - janela da base de conhecimento
 */
public class ChatController {

    private final Main app;
    private final ChatView tela;
    private final UsuarioService usuarioService;
    private final ConversaService conversaService;
    private final ChatService chatService;
    private final RagService ragService;
    private final Usuario usuario;

    private final ObservableList<Conversa> conversas = FXCollections.observableArrayList();
    private final FilteredList<Conversa> conversasFiltradas = new FilteredList<>(conversas, c -> true);
    private Conversa conversaAtual;
    private boolean enviando;
    private boolean ignorarSelecao;

    public ChatController(Main app, UsuarioService usuarioService, ConversaService conversaService, ChatService chatService,
                          RagService ragService, TemaController temaController) {
        this.app = app;
        this.usuarioService = usuarioService;
        this.conversaService = conversaService;
        this.chatService = chatService;
        this.ragService = ragService;
        this.usuario = usuarioService.getUsuarioLogado();
        this.tela = new ChatView(usuario.getNome(), usuario.getEmail());

        temaController.configurarBotao(tela.botaoTema);
        tela.saudacao.setText("Olá, " + usuario.primeiroNome() + "! Como posso ajudar?");
        for (String sugestao : ChatView.SUGESTOES) {
            Button botao = new Button(sugestao, Componentes.icone(Componentes.BRILHO, 12));
            botao.getStyleClass().add("suggestion-chip");
            botao.setOnAction(e -> {
                tela.campoMensagem.setText(sugestao);
                enviarMensagem();
            });
            tela.sugestoes.getChildren().add(botao);
        }

        // barra lateral
        tela.listaConversas.setItems(conversasFiltradas);
        tela.listaConversas.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (!ignorarSelecao && nova != null && (antiga == null || !nova.getId().equals(antiga.getId()))) {
                abrirConversa(nova);
            }
        });
        tela.campoBusca.textProperty().addListener((obs, antigo, busca) -> conversasFiltradas.setPredicate(
                c -> busca.isBlank() || c.getTitulo().toLowerCase().contains(busca.trim().toLowerCase())));
        tela.botaoNovaConversa.setOnAction(e -> criarConversa());
        tela.aoRenomear = this::renomearConversa;
        tela.aoExcluir = this::excluirConversa;
        tela.botaoBaseConhecimento.setOnAction(e -> abrirBaseConhecimento());
        tela.botaoSair.setOnAction(e -> {
            usuarioService.fazerLogout();
            app.mostrarLogin(null, "Você saiu da sua conta.");
        });

        // chat
        tela.botaoEnviar.setOnAction(e -> enviarMensagem());
        tela.campoMensagem.addEventFilter(KeyEvent.KEY_PRESSED, this::apertouEnter);
        tela.botaoMenu.setOnAction(e -> tela.barraLateral.setVisible(!tela.barraLateral.isVisible()));
        tela.botaoRenomear.setOnAction(e -> renomearConversa(conversaAtual));
        tela.botaoExcluir.setOnAction(e -> excluirConversa(conversaAtual));

        abrirConversa(null);
        carregarConversas();
    }

    public ChatView getTela() {
        return tela;
    }

    // ---------- conversas ----------

    private void carregarConversas() {
        Componentes.emSegundoPlano(conversaService::listarConversas, lista -> {
            conversas.setAll(lista);
            if (!lista.isEmpty()) tela.listaConversas.getSelectionModel().selectFirst();
        }, tela.aviso::erro);
    }

    private void criarConversa() {
        tela.botaoNovaConversa.setDisable(true);
        Componentes.emSegundoPlano(conversaService::criarConversa, nova -> {
            tela.botaoNovaConversa.setDisable(false);
            tela.campoBusca.clear();
            conversas.add(0, nova);
            tela.listaConversas.getSelectionModel().select(nova);
        }, erro -> {
            tela.botaoNovaConversa.setDisable(false);
            tela.aviso.erro(erro);
        });
    }

    private void renomearConversa(Conversa conversa) {
        if (conversa == null) return;
        Componentes.perguntarTexto(janela(), "Renomear conversa", "Novo título:", conversa.getTitulo())
                .filter(titulo -> !titulo.isBlank() && !titulo.equals(conversa.getTitulo()))
                .ifPresent(titulo -> Componentes.emSegundoPlano(
                        () -> conversaService.renomearConversa(conversa.getId(), titulo),
                        atualizada -> {
                            atualizarNaLista(atualizada, false);
                            if (conversaAtual != null && conversaAtual.getId().equals(atualizada.getId())) {
                                conversaAtual = atualizada;
                                atualizarTopo();
                            }
                        },
                        erro -> Componentes.mostrarErro(janela(), "Não foi possível renomear", erro)));
    }

    private void excluirConversa(Conversa conversa) {
        if (conversa == null) return;
        boolean confirmou = Componentes.confirmar(janela(), "Excluir conversa?", "A conversa \"" + conversa.getTitulo()
                + "\" e todas as mensagens dela serão apagadas para sempre.", "Excluir");
        if (!confirmou) return;
        Componentes.emSegundoPlano(() -> {
            conversaService.excluirConversa(conversa.getId());
            return conversa;
        }, excluida -> {
            conversas.removeIf(c -> c.getId().equals(excluida.getId()));
            if (conversaAtual != null && conversaAtual.getId().equals(excluida.getId())) {
                abrirConversa(null);
                if (!conversas.isEmpty()) tela.listaConversas.getSelectionModel().selectFirst();
            }
        }, erro -> Componentes.mostrarErro(janela(), "Erro ao excluir conversa", erro));
    }

    // atualiza a conversa na lista sem recarregar o chat (paraOTopo = acabou de ser usada)
    private void atualizarNaLista(Conversa conversa, boolean paraOTopo) {
        ignorarSelecao = true;
        Conversa selecionada = tela.listaConversas.getSelectionModel().getSelectedItem();
        int posicao = -1;
        for (int i = 0; i < conversas.size(); i++) {
            if (conversas.get(i).getId().equals(conversa.getId())) posicao = i;
        }
        if (posicao >= 0 && !paraOTopo) {
            conversas.set(posicao, conversa);
        } else {
            if (posicao >= 0) conversas.remove(posicao);
            conversas.add(0, conversa);
        }
        if (selecionada == null || selecionada.getId().equals(conversa.getId())) {
            tela.listaConversas.getSelectionModel().select(conversa);
        }
        ignorarSelecao = false;
    }

    // ---------- chat ----------

    private void abrirConversa(Conversa conversa) {
        conversaAtual = conversa;
        tela.aviso.esconder();
        tela.mensagens.getChildren().clear();
        atualizarTopo();
        if (conversa == null) {
            tela.mostrarTelaVazia(true);
            return;
        }
        tela.mostrarCarregando(true);
        String id = conversa.getId();
        Componentes.emSegundoPlano(() -> conversaService.listarMensagens(id), mensagens -> {
            if (conversaAtual == null || !conversaAtual.getId().equals(id)) return; // já trocou de conversa
            tela.mostrarCarregando(false);
            tela.mensagens.getChildren().clear();
            tela.mostrarTelaVazia(mensagens.isEmpty());
            mensagens.forEach(m -> tela.adicionarMensagem(balao(m)));
        }, erro -> {
            tela.mostrarCarregando(false);
            tela.mostrarTelaVazia(false);
            tela.aviso.erro(erro);
        });
        tela.campoMensagem.requestFocus();
    }

    // Enter envia, Shift+Enter pula linha
    private void apertouEnter(KeyEvent evento) {
        if (evento.getCode() != KeyCode.ENTER) return;
        evento.consume();
        if (evento.isShiftDown()) {
            tela.campoMensagem.insertText(tela.campoMensagem.getCaretPosition(), "\n");
        } else {
            enviarMensagem();
        }
    }

    private void enviarMensagem() {
        String texto = tela.campoMensagem.getText();
        if (enviando || texto == null || texto.isBlank()) return;
        if (texto.length() > ChatService.TAMANHO_MAXIMO) {
            tela.aviso.erro("A mensagem deve ter no máximo " + ChatService.TAMANHO_MAXIMO + " caracteres.");
            return;
        }
        mostrarEnviando(true);
        tela.aviso.esconder();

        // mostra a pergunta na hora; se a IA der erro, ela some e o texto volta para o campo
        MensagemView pergunta = balao(Mensagem.builder().autor(Mensagem.Autor.USUARIO).conteudo(texto.trim()).build());
        pergunta.setOpacity(0.75);
        tela.adicionarMensagem(pergunta);
        tela.campoMensagem.clear();

        Conversa conversaDoEnvio = conversaAtual;
        Componentes.emSegundoPlano(() -> {
            Conversa conversa = conversaDoEnvio != null ? conversaDoEnvio : conversaService.criarConversa();
            return chatService.enviarMensagem(conversa.getId(), texto);
        }, (RespostaChatDTO resposta) -> {
            mostrarEnviando(false);
            atualizarNaLista(resposta.conversa(), true);
            if (conversaAtual != null && !conversaAtual.getId().equals(resposta.conversa().getId())) return;
            conversaAtual = resposta.conversa();
            atualizarTopo();
            tela.mensagens.getChildren().remove(pergunta); // troca pela versão salva (com horário)
            tela.adicionarMensagem(balao(resposta.pergunta()));
            tela.adicionarMensagem(balao(resposta.resposta()));
            tela.campoMensagem.requestFocus();
        }, erro -> {
            mostrarEnviando(false);
            tela.mensagens.getChildren().remove(pergunta);
            if (conversaAtual == conversaDoEnvio) tela.campoMensagem.setText(texto);
            tela.aviso.erro(erro);
        });
    }

    private void mostrarEnviando(boolean valor) {
        enviando = valor;
        tela.botaoEnviar.setDisable(valor);
        tela.digitando.mostrar(valor);
    }

    private MensagemView balao(Mensagem mensagem) {
        MensagemView balao = new MensagemView(mensagem, usuario.getNome(), tela.rolagemMensagens.widthProperty());
        balao.animarEntrada();
        return balao;
    }

    private void atualizarTopo() {
        boolean temConversa = conversaAtual != null;
        tela.tituloConversa.setText(temConversa ? conversaAtual.getTitulo() : "Nova conversa");
        tela.dataConversa.setText(temConversa ? "Criada em " + Componentes.formatarData(conversaAtual.getCriadoEm())
                : "Envie uma mensagem para começar");
        tela.botaoRenomear.setDisable(!temConversa);
        tela.botaoExcluir.setDisable(!temConversa);
    }

    private Window janela() {
        return tela.getRaiz().getScene().getWindow();
    }

    // ---------- base de conhecimento ----------

    private void abrirBaseConhecimento() {
        BaseConhecimentoView base = new BaseConhecimentoView();
        Stage janela = app.abrirJanela(base.getRaiz(), "Base de conhecimento", 980, 640);
        base.botaoFechar.setOnAction(e -> janela.close());
        base.botaoNovo.setOnAction(e -> {
            base.listaDocumentos.getSelectionModel().clearSelection();
            base.mostrarNovoDocumento();
        });
        base.listaDocumentos.getSelectionModel().selectedItemProperty().addListener((obs, antigo, documento) -> {
            if (documento != null) base.mostrarDocumento(documento, Objects.equals(documento.getDonoId(), usuario.getId()));
        });
        base.botaoSalvar.setOnAction(e -> salvarDocumento(base));
        base.botaoExcluir.setOnAction(e -> excluirDocumento(base, janela));
        base.botaoPesquisar.setOnAction(e -> testarBusca(base));
        base.campoPergunta.setOnAction(e -> testarBusca(base));
        carregarDocumentos(base, null);
        janela.showAndWait();
    }

    private void carregarDocumentos(BaseConhecimentoView base, String selecionarId) {
        Componentes.emSegundoPlano(ragService::listarDocumentos, documentos -> {
            base.listaDocumentos.getItems().setAll(documentos);
            documentos.stream().filter(d -> d.getId().equals(selecionarId)).findFirst()
                    .ifPresent(d -> base.listaDocumentos.getSelectionModel().select(d));
        }, base.aviso::erro);
    }

    private void salvarDocumento(BaseConhecimentoView base) {
        if (base.titulo.getValor().isBlank()) {
            base.titulo.mostrarErro("Informe o título.");
            return;
        }
        base.botaoSalvar.carregando("Salvando…");
        Componentes.emSegundoPlano(
                () -> ragService.cadastrarDocumento(base.titulo.getValor(), base.conteudo.getText(), base.palavrasChave.getValor()),
                salvo -> {
                    base.botaoSalvar.pronto();
                    carregarDocumentos(base, salvo.getId());
                    base.aviso.sucesso("Documento salvo com " + salvo.getPalavrasChave().size() + " palavras-chave.");
                }, erro -> {
                    base.botaoSalvar.pronto();
                    base.aviso.erro(erro);
                });
    }

    private void excluirDocumento(BaseConhecimentoView base, Stage janela) {
        Documento documento = base.listaDocumentos.getSelectionModel().getSelectedItem();
        if (documento == null || !Componentes.confirmar(janela, "Excluir documento?",
                "O documento \"" + documento.getTitulo() + "\" não será mais usado pelo RAG.", "Excluir")) {
            return;
        }
        Componentes.emSegundoPlano(() -> {
            ragService.excluirDocumento(documento.getId());
            return true;
        }, ok -> {
            base.mostrarNovoDocumento();
            carregarDocumentos(base, null);
            base.aviso.sucesso("Documento excluído.");
        }, base.aviso::erro);
    }

    // roda o mesmo RAG do chat e mostra cada etapa
    private void testarBusca(BaseConhecimentoView base) {
        String pergunta = base.campoPergunta.getText();
        if (pergunta == null || pergunta.isBlank()) return;
        List<String> removidas = ProcessadorTexto.tokenizar(ProcessadorTexto.normalizar(pergunta)).stream()
                .filter(ProcessadorTexto::isStopWord).distinct().toList();

        base.botaoPesquisar.setDisable(true);
        Componentes.emSegundoPlano(() -> ragService.buscarContexto(pergunta), resultado -> {
            base.botaoPesquisar.setDisable(false);
            mostrarEtiquetas(base.palavrasEncontradas.getChildren(), resultado.palavrasChave(), "chip-keyword");
            mostrarEtiquetas(base.stopWordsRemovidas.getChildren(), removidas, "chip-stopword");
            base.resultados.getChildren().clear();
            if (!resultado.temContexto()) {
                base.resultados.getChildren().add(textoCinza("Nenhum documento encontrado. A IA vai responder só "
                        + "com o conhecimento dela."));
            }
            int posicao = 1;
            for (ResultadoRagDTO.DocumentoEncontrado item : resultado.documentos()) {
                String trecho = item.documento().getConteudo();
                VBox cartao = new VBox(4,
                        Componentes.texto(posicao++ + ". " + item.documento().getTitulo(), "settings-strong"),
                        textoCinza("Pontuação: " + item.pontuacao() + "   ·   palavras encontradas: "
                                + String.join(", ", item.termosEncontrados())),
                        textoCinza(trecho.length() > 220 ? trecho.substring(0, 220) + "…" : trecho));
                cartao.getStyleClass().add("result-card");
                base.resultados.getChildren().add(cartao);
            }
        }, erro -> {
            base.botaoPesquisar.setDisable(false);
            base.resultados.getChildren().setAll(textoCinza(erro));
        });
    }

    private void mostrarEtiquetas(List<Node> destino, List<String> palavras, String estilo) {
        destino.clear();
        palavras.forEach(p -> destino.add(BaseConhecimentoView.etiqueta(p, estilo)));
        if (palavras.isEmpty()) destino.add(textoCinza("Nenhuma."));
    }

    private static Label textoCinza(String texto) {
        Label label = Componentes.texto(texto, "muted");
        label.setWrapText(true);
        return label;
    }
}
