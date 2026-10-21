package com.matheushenrique.lumi.controller;

import com.matheushenrique.lumi.Main;
import com.matheushenrique.lumi.business.RecuperacaoSenhaService;
import com.matheushenrique.lumi.business.UsuarioService;
import com.matheushenrique.lumi.dto.CadastroDTO;
import com.matheushenrique.lumi.view.Componentes;
import com.matheushenrique.lumi.view.LoginView;
import javafx.scene.Parent;

// ações das telas de login, cadastro e "esqueci minha senha" (as regras ficam nos services)
public class LoginController {

    private final Main app;
    private final UsuarioService usuarioService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final TemaController temaController;

    public LoginController(Main app, UsuarioService usuarioService, RecuperacaoSenhaService recuperacaoSenhaService,
                           TemaController temaController) {
        this.app = app;
        this.usuarioService = usuarioService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
        this.temaController = temaController;
    }

    // ---------- login ----------

    public Parent telaLogin(String email, String mensagem) {
        LoginView tela = criarTela(LoginView.Tipo.LOGIN);
        tela.linkTrocarTela.setOnAction(e -> app.mostrarCadastro());
        tela.linkEsqueciSenha.setOnAction(e -> app.mostrarRecuperarSenha(tela.email.getValor()));
        tela.botaoPrincipal.setOnAction(e -> entrar(tela));
        if (email != null) tela.email.getEntrada().setText(email);
        if (mensagem != null) tela.aviso.sucesso(mensagem);
        return tela.getRaiz();
    }

    private void entrar(LoginView tela) {
        tela.aviso.esconder();
        boolean ok = conferirEmail(tela);
        if (tela.senha.getValor().isEmpty()) {
            tela.senha.mostrarErro("Informe a sua senha.");
            ok = false;
        }
        if (!ok) return;

        tela.botaoPrincipal.carregando("Entrando…");
        Componentes.emSegundoPlano(() -> usuarioService.fazerLogin(tela.email.getValor(), tela.senha.getValor()),
                usuario -> app.mostrarChat(),
                erro -> {
                    tela.botaoPrincipal.pronto();
                    tela.aviso.erro(erro);
                    tela.senha.getEntrada().clear();
                });
    }

    // ---------- cadastro ----------

    public Parent telaCadastro() {
        LoginView tela = criarTela(LoginView.Tipo.CADASTRO);
        tela.linkTrocarTela.setOnAction(e -> app.mostrarLogin(null, null));
        tela.botaoPrincipal.setOnAction(e -> cadastrar(tela));
        tela.senha.getEntrada().textProperty().addListener((obs, antigo, senha) -> mostrarForcaSenha(tela, senha));
        return tela.getRaiz();
    }

    private void cadastrar(LoginView tela) {
        tela.aviso.esconder();
        boolean ok = conferirEmail(tela);
        if (tela.nome.getValor().isBlank()) {
            tela.nome.mostrarErro("Informe o seu nome.");
            ok = false;
        }
        if (tela.senha.getValor().length() < 8) {
            tela.senha.mostrarErro("A senha deve ter pelo menos 8 caracteres.");
            ok = false;
        }
        if (!tela.senha.getValor().equals(tela.confirmarSenha.getValor())) {
            tela.confirmarSenha.mostrarErro("As senhas não são iguais.");
            ok = false;
        }
        if (!ok) return;

        CadastroDTO dto = new CadastroDTO(tela.nome.getValor(), tela.email.getValor(), tela.senha.getValor(),
                tela.confirmarSenha.getValor());
        tela.botaoPrincipal.carregando("Criando conta…");
        Componentes.emSegundoPlano(() -> usuarioService.cadastrarUsuario(dto),
                usuario -> app.mostrarLogin(usuario.getEmail(), "Conta criada com sucesso! Agora é só entrar."),
                erro -> {
                    tela.botaoPrincipal.pronto();
                    tela.aviso.erro(erro);
                });
    }

    private void mostrarForcaSenha(LoginView tela, String senha) {
        int pontos = 0;
        if (senha.length() >= 8) pontos++;
        if (senha.length() >= 12) pontos++;
        if (senha.matches(".*[A-Za-z].*") && senha.matches(".*\\d.*")) pontos++;
        if (senha.matches(".*[A-Z].*") && senha.matches(".*[a-z].*")) pontos++;
        if (senha.matches(".*[^A-Za-z0-9].*")) pontos++;
        String[] nomes = {"Muito fraca", "Fraca", "Razoável", "Boa", "Forte", "Muito forte"};
        tela.forcaSenha.setProgress(senha.isEmpty() ? 0 : Math.max(0.1, pontos / 5.0));
        tela.textoForcaSenha.setText(senha.isEmpty() ? " " : "Força da senha: " + nomes[pontos]);
        tela.forcaSenha.getStyleClass().removeAll("weak", "medium", "strong");
        tela.forcaSenha.getStyleClass().add(pontos <= 1 ? "weak" : pontos <= 3 ? "medium" : "strong");
    }

    // ---------- recuperar senha ----------

    public Parent telaRecuperarSenha(String email) {
        LoginView tela = criarTela(LoginView.Tipo.RECUPERAR_SENHA);
        if (email != null) tela.email.getEntrada().setText(email);
        tela.linkTrocarTela.setOnAction(e -> app.mostrarLogin(null, null));
        tela.botaoEnviarCodigo.setOnAction(e -> enviarCodigo(tela));
        tela.linkReenviarCodigo.setOnAction(e -> {
            tela.voltarEtapaEmail();
            enviarCodigo(tela);
        });
        tela.botaoPrincipal.setOnAction(e -> redefinirSenha(tela));
        return tela.getRaiz();
    }

    private void enviarCodigo(LoginView tela) {
        tela.aviso.esconder();
        if (!conferirEmail(tela)) return;
        tela.botaoEnviarCodigo.carregando("Gerando código…");
        Componentes.emSegundoPlano(() -> recuperacaoSenhaService.solicitarCodigo(tela.email.getValor()), codigoDemo -> {
            tela.botaoEnviarCodigo.pronto();
            int minutos = RecuperacaoSenhaService.MINUTOS_VALIDADE;
            if (codigoDemo.isPresent()) {
                tela.aviso.info("Modo demonstração (e-mail não configurado). Seu código é: " + codigoDemo.get());
            } else {
                tela.aviso.sucesso("Enviamos um código para o seu e-mail.");
            }
            tela.mostrarEtapaCodigo("O código vale por " + minutos + " minutos e só pode ser usado uma vez.");
        }, erro -> {
            tela.botaoEnviarCodigo.pronto();
            tela.aviso.erro(erro);
        });
    }

    private void redefinirSenha(LoginView tela) {
        tela.aviso.esconder();
        if (tela.codigo.getValor().isBlank()) {
            tela.codigo.mostrarErro("Informe o código.");
            return;
        }
        if (!tela.senha.getValor().equals(tela.confirmarSenha.getValor())) {
            tela.confirmarSenha.mostrarErro("As senhas não são iguais.");
            return;
        }
        String email = tela.email.getValor();
        tela.botaoPrincipal.carregando("Salvando…");
        Componentes.emSegundoPlano(() -> {
            recuperacaoSenhaService.redefinirSenha(email, tela.codigo.getValor(), tela.senha.getValor(),
                    tela.confirmarSenha.getValor());
            return true;
        }, ok -> app.mostrarLogin(email, "Senha alterada! Entre com a senha nova."), erro -> {
            tela.botaoPrincipal.pronto();
            tela.aviso.erro(erro);
        });
    }

    // ---------- auxiliares ----------

    private LoginView criarTela(LoginView.Tipo tipo) {
        LoginView tela = new LoginView(tipo);
        temaController.configurarBotao(tela.botaoTema);
        return tela;
    }

    private boolean conferirEmail(LoginView tela) {
        if (tela.email.getValor().isBlank()) {
            tela.email.mostrarErro("Informe o seu e-mail.");
            return false;
        }
        if (!UsuarioService.emailValido(tela.email.getValor())) {
            tela.email.mostrarErro("E-mail em formato inválido.");
            return false;
        }
        return true;
    }
}
