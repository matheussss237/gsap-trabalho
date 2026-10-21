package com.matheushenrique.lumi.business;

import com.matheushenrique.lumi.dto.CadastroDTO;
import com.matheushenrique.lumi.exceptions.RegraNegocioException;
import com.matheushenrique.lumi.infrastructure.entitys.Usuario;
import com.matheushenrique.lumi.infrastructure.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

// cadastro, login, logout e sessão do usuário
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // sessão: quem está logado agora (os outros services sempre pegam o dono dos dados daqui)
    private volatile Usuario usuarioLogado;

    public Usuario cadastrarUsuario(CadastroDTO dto) {
        String nome = obrigatorio(dto.nome(), "Nome");
        String email = validarEmail(dto.email());
        validarSenha(dto.senha(), dto.confirmacaoSenha());
        if (nome.length() < 2) {
            throw new RegraNegocioException("O nome deve ter pelo menos 2 caracteres.");
        }
        if (repository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe uma conta cadastrada com este e-mail.");
        }

        Usuario usuario = Usuario.builder()
                .nome(nome)
                .email(email)
                .senha(encoder.encode(dto.senha())) // senha nunca é salva em texto puro
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
        return repository.save(usuario);
    }

    public Usuario fazerLogin(String email, String senha) {
        if (senha == null || senha.isEmpty()) {
            throw new RegraNegocioException("Informe a sua senha.");
        }
        Usuario usuario = repository.findByEmail(validarEmail(email))
                .orElseThrow(() -> new RegraNegocioException("Não existe usuário cadastrado com este e-mail."));
        if (!encoder.matches(senha, usuario.getSenha())) {
            throw new RegraNegocioException("Senha incorreta. Tente de novo ou use \"Esqueci minha senha\".");
        }
        usuarioLogado = usuario;
        return usuario;
    }

    public void fazerLogout() {
        usuarioLogado = null;
    }

    public Usuario getUsuarioLogado() {
        if (usuarioLogado == null) {
            throw new RegraNegocioException("Sua sessão foi encerrada. Faça login novamente.");
        }
        return usuarioLogado;
    }

    public String getIdUsuarioLogado() {
        return getUsuarioLogado().getId();
    }

    public boolean estaLogado() {
        return usuarioLogado != null;
    }

    public Optional<Usuario> buscarUsuarioPorEmail(String email) {
        return repository.findByEmail(validarEmail(email));
    }

    public void atualizarSenha(Usuario usuario, String novaSenha) {
        usuario.setSenha(encoder.encode(novaSenha));
        usuario.setAtualizadoEm(LocalDateTime.now());
        repository.save(usuario);
    }

    // ---------- validações ----------

    public static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException("O campo \"" + campo + "\" é obrigatório.");
        }
        return valor.trim();
    }

    public static boolean emailValido(String email) {
        return email != null && email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    }

    public static String validarEmail(String email) {
        String valor = obrigatorio(email, "E-mail");
        if (!emailValido(valor)) {
            throw new RegraNegocioException("Informe um e-mail válido, por exemplo: nome@empresa.com.");
        }
        return valor.toLowerCase();
    }

    public static void validarSenha(String senha, String confirmacao) {
        if (senha == null || senha.length() < 8) {
            throw new RegraNegocioException("A senha deve ter pelo menos 8 caracteres.");
        }
        if (!senha.matches(".*[A-Za-z].*") || !senha.matches(".*\\d.*")) {
            throw new RegraNegocioException("A senha deve ter letras e números.");
        }
        if (!senha.equals(confirmacao)) {
            throw new RegraNegocioException("A confirmação da senha não confere.");
        }
    }
}
