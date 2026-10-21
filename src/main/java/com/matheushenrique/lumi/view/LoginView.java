package com.matheushenrique.lumi.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

/*
 * Telas de login, cadastro e recuperação de senha (feitas só com código, sem Scene Builder).
 * O painel roxo da esquerda some quando a janela fica estreita.
 */
public class LoginView {

    public enum Tipo { LOGIN, CADASTRO, RECUPERAR_SENHA }

    public final Componentes.Campo nome = new Componentes.Campo("Nome", "Seu nome", false);
    public final Componentes.Campo email = new Componentes.Campo("E-mail", "nome@empresa.com", false);
    public final Componentes.Campo senha;
    public final Componentes.Campo confirmarSenha;
    public final Componentes.Campo codigo = new Componentes.Campo("Código de verificação", "6 dígitos", false);
    public final ProgressBar forcaSenha = new ProgressBar(0);
    public final Label textoForcaSenha = Componentes.texto(" ", "strength-label");
    public final Componentes.BotaoCarregando botaoPrincipal;
    public final Componentes.BotaoCarregando botaoEnviarCodigo = new Componentes.BotaoCarregando("Enviar código");
    public final Button linkEsqueciSenha = link("Esqueci minha senha");
    public final Button linkReenviarCodigo = link("Reenviar código");
    public final Button linkTrocarTela;
    public final Button botaoTema = Componentes.botaoIcone(Componentes.SOL, "Tema claro/escuro");
    public final Componentes.Aviso aviso = new Componentes.Aviso();

    private final Label explicacaoCodigo = Componentes.texto("", "muted");
    private final VBox etapaCodigo = new VBox(14);
    private final HBox raiz = new HBox();

    public LoginView(Tipo tipo) {
        boolean recuperar = tipo == Tipo.RECUPERAR_SENHA;
        senha = new Componentes.Campo(recuperar ? "Nova senha" : "Senha",
                tipo == Tipo.LOGIN ? "Sua senha" : "Mínimo de 8 caracteres, com letras e números", true);
        confirmarSenha = new Componentes.Campo(recuperar ? "Confirmar nova senha" : "Confirmar senha", "Repita a senha", true);
        botaoPrincipal = new Componentes.BotaoCarregando(
                tipo == Tipo.LOGIN ? "Entrar" : tipo == Tipo.CADASTRO ? "Criar conta" : "Redefinir senha");
        linkTrocarTela = link(tipo == Tipo.LOGIN ? "Criar conta" : tipo == Tipo.CADASTRO ? "Entrar" : "Voltar ao login");

        VBox cartao = new VBox(18);
        cartao.getStyleClass().add("auth-card");
        cartao.setMaxWidth(420);
        cartao.setMaxHeight(Region.USE_PREF_SIZE);

        if (tipo == Tipo.LOGIN) {
            HBox linhaEsqueci = new HBox(linkEsqueciSenha);
            linhaEsqueci.setAlignment(Pos.CENTER_RIGHT);
            botaoPrincipal.setDefaultButton(true);
            cartao.getChildren().addAll(titulo("Bem-vindo de volta", "Entre com seu e-mail e senha para continuar."),
                    aviso, email, senha, linhaEsqueci, botaoPrincipal, linhaLink("Não tem uma conta?"));
        } else if (tipo == Tipo.CADASTRO) {
            forcaSenha.getStyleClass().add("strength-bar");
            forcaSenha.setMaxWidth(Double.MAX_VALUE);
            botaoPrincipal.setDefaultButton(true);
            cartao.getChildren().addAll(titulo("Criar conta", "Cadastre-se para conversar com a Lumi."),
                    aviso, nome, email, senha, new VBox(4, forcaSenha, textoForcaSenha), confirmarSenha,
                    botaoPrincipal, linhaLink("Já possui conta?"));
        } else {
            explicacaoCodigo.setWrapText(true);
            etapaCodigo.getChildren().addAll(explicacaoCodigo, codigo, senha, confirmarSenha, botaoPrincipal, linkReenviarCodigo);
            etapaCodigo.managedProperty().bind(etapaCodigo.visibleProperty());
            etapaCodigo.setVisible(false);
            botaoEnviarCodigo.managedProperty().bind(botaoEnviarCodigo.visibleProperty());
            botaoEnviarCodigo.setDefaultButton(true);
            cartao.getChildren().addAll(titulo("Recuperar senha",
                            "Informe seu e-mail. Você vai receber um código temporário para criar uma senha nova."),
                    aviso, email, botaoEnviarCodigo, etapaCodigo, linhaLink("Lembrou a senha?"));
        }

        StackPane centro = new StackPane(cartao);
        centro.setPadding(new Insets(10, 32, 32, 32));
        ScrollPane rolagem = new ScrollPane(centro);
        rolagem.setFitToWidth(true);
        rolagem.setFitToHeight(true);
        rolagem.getStyleClass().add("transparent-scroll");
        HBox topo = new HBox(botaoTema);
        topo.setAlignment(Pos.TOP_RIGHT);
        topo.setPadding(new Insets(16, 20, 0, 0));
        BorderPane ladoFormulario = new BorderPane(rolagem);
        ladoFormulario.setTop(topo);
        HBox.setHgrow(ladoFormulario, Priority.ALWAYS);

        VBox painelMarca = painelMarca();
        painelMarca.managedProperty().bind(painelMarca.visibleProperty());
        raiz.widthProperty().addListener((obs, antigo, largura) -> painelMarca.setVisible(largura.doubleValue() >= 860));
        raiz.getStyleClass().add("auth-root");
        raiz.getChildren().addAll(painelMarca, ladoFormulario);
    }

    // recuperação de senha: mostra os campos do código e da senha nova
    public void mostrarEtapaCodigo(String explicacao) {
        explicacaoCodigo.setText(explicacao);
        email.getEntrada().setDisable(true);
        botaoEnviarCodigo.setVisible(false);
        botaoEnviarCodigo.setDefaultButton(false);
        botaoPrincipal.setDefaultButton(true);
        etapaCodigo.setVisible(true);
        codigo.getEntrada().requestFocus();
    }

    public void voltarEtapaEmail() {
        email.getEntrada().setDisable(false);
        botaoEnviarCodigo.setVisible(true);
        botaoEnviarCodigo.setDefaultButton(true);
        botaoPrincipal.setDefaultButton(false);
        etapaCodigo.setVisible(false);
    }

    public Parent getRaiz() {
        return raiz;
    }

    private static Node titulo(String titulo, String subtitulo) {
        Label sub = Componentes.texto(subtitulo, "auth-subtitle");
        sub.setWrapText(true);
        return new VBox(6, Componentes.texto(titulo, "auth-title"), sub);
    }

    private HBox linhaLink(String texto) {
        HBox linha = new HBox(4, Componentes.texto(texto, "muted"), linkTrocarTela);
        linha.setAlignment(Pos.CENTER);
        return linha;
    }

    private static Button link(String texto) {
        Button botao = new Button(texto);
        botao.getStyleClass().add("link-button");
        return botao;
    }

    private static VBox painelMarca() {
        Label frase = Componentes.texto("Sua assistente de IA que conhece a empresa.", "brand-tagline");
        frase.setWrapText(true);
        VBox vantagens = new VBox(14,
                vantagem(Componentes.BALAO, "Conversas organizadas", "Histórico salvo no MongoDB Atlas, só seu."),
                vantagem(Componentes.LIVRO, "RAG transparente", "Veja quais documentos embasaram cada resposta."),
                vantagem(Componentes.BRILHO, "Feita para você", "A Lumi te chama pelo nome e lembra da conversa."));
        VBox.setMargin(vantagens, new Insets(22, 0, 0, 0));
        Region espaco = new Region();
        VBox.setVgrow(espaco, Priority.ALWAYS);
        VBox painel = new VBox(18, Componentes.logo(64), Componentes.texto("Lumi", "brand-name"), frase, vantagens,
                espaco, Componentes.texto("v1.0.0 · JavaFX · Spring Boot · MongoDB · Groq", "brand-footer"));
        painel.getStyleClass().add("brand-panel");
        painel.setPrefWidth(400);
        painel.setMinWidth(360);
        return painel;
    }

    private static Node vantagem(String svg, String titulo, String descricao) {
        StackPane caixaIcone = new StackPane(Componentes.icone(svg, 16));
        caixaIcone.getStyleClass().add("feature-icon");
        Label texto = Componentes.texto(descricao, "feature-description");
        texto.setWrapText(true);
        HBox linha = new HBox(14, caixaIcone, new VBox(2, Componentes.texto(titulo, "feature-title"), texto));
        linha.setAlignment(Pos.CENTER_LEFT);
        return linha;
    }
}
