package com.matheushenrique.lumi.business;

import com.matheushenrique.lumi.exceptions.RegraNegocioException;
import com.matheushenrique.lumi.infrastructure.entitys.TokenRecuperacao;
import com.matheushenrique.lumi.infrastructure.entitys.Usuario;
import com.matheushenrique.lumi.infrastructure.repository.TokenRecuperacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/*
 * "Esqueci minha senha" com código temporário:
 * - a senha antiga nunca é mostrada (no banco só existe o hash BCrypt)
 * - código de 6 dígitos, vale 15 minutos e só pode ser usado uma vez
 * - no banco fica só o SHA-256 do código
 * - 5 tentativas erradas invalidam o código
 * Se o e-mail (SPRING_MAIL_*) estiver configurado, o código vai por e-mail;
 * senão o app está em "modo demonstração" e o código aparece na tela.
 */
@Service
@RequiredArgsConstructor
public class RecuperacaoSenhaService {

    public static final int MINUTOS_VALIDADE = 15;
    public static final int MAXIMO_TENTATIVAS = 5;

    private final UsuarioService usuarioService;
    private final TokenRecuperacaoRepository repository;
    private final ObjectProvider<JavaMailSender> email;
    private final SecureRandom random = new SecureRandom();
    private Clock relogio = Clock.systemDefaultZone();

    // devolve o código só quando estiver em modo demonstração (sem e-mail configurado)
    public Optional<String> solicitarCodigo(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);

        // um código novo cancela os anteriores
        repository.findByUsuarioIdAndUsadoFalse(usuario.getId()).forEach(token -> {
            token.setUsado(true);
            repository.save(token);
        });

        String codigo = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime agora = LocalDateTime.now(relogio);
        repository.save(TokenRecuperacao.builder()
                .usuarioId(usuario.getId())
                .codigoHash(sha256(codigo))
                .criadoEm(agora)
                .expiraEm(agora.plusMinutes(MINUTOS_VALIDADE))
                .build());

        JavaMailSender enviador = email.getIfAvailable();
        if (enviador == null) {
            return Optional.of(codigo);
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(usuario.getEmail());
        mensagem.setSubject("Lumi - código de recuperação de senha");
        mensagem.setText("Olá, " + usuario.primeiroNome() + "!\n\nSeu código é: " + codigo
                + "\nEle vale por " + MINUTOS_VALIDADE + " minutos e só pode ser usado uma vez.");
        try {
            enviador.send(mensagem);
        } catch (RuntimeException e) {
            throw new RegraNegocioException("Não foi possível enviar o e-mail. Tente novamente mais tarde.", e);
        }
        return Optional.empty();
    }

    public void redefinirSenha(String emailUsuario, String codigo, String novaSenha, String confirmacao) {
        UsuarioService.obrigatorio(codigo, "Código");
        UsuarioService.validarSenha(novaSenha, confirmacao);
        Usuario usuario = buscarUsuario(emailUsuario);
        TokenRecuperacao token = repository.findFirstByUsuarioIdAndUsadoFalseOrderByCriadoEmDesc(usuario.getId())
                .orElseThrow(() -> new RegraNegocioException("Nenhum código ativo. Peça um novo código."));

        if (!LocalDateTime.now(relogio).isBefore(token.getExpiraEm())) {
            token.setUsado(true);
            repository.save(token);
            throw new RegraNegocioException("O código expirou. Peça um novo código.");
        }
        boolean codigoCerto = MessageDigest.isEqual(
                token.getCodigoHash().getBytes(StandardCharsets.UTF_8),
                sha256(codigo.trim()).getBytes(StandardCharsets.UTF_8));
        if (!codigoCerto) {
            token.setTentativas(token.getTentativas() + 1);
            token.setUsado(token.getTentativas() >= MAXIMO_TENTATIVAS);
            repository.save(token);
            throw new RegraNegocioException(token.isUsado()
                    ? "Muitas tentativas erradas. Peça um novo código."
                    : "Código inválido. Restam " + (MAXIMO_TENTATIVAS - token.getTentativas()) + " tentativa(s).");
        }

        usuarioService.atualizarSenha(usuario, novaSenha);
        token.setUsado(true);
        repository.save(token);
    }

    public boolean emailConfigurado() {
        return email.getIfAvailable() != null;
    }

    // usado nos testes para simular a passagem do tempo
    void setRelogio(Clock relogio) {
        this.relogio = relogio;
    }

    private Usuario buscarUsuario(String emailUsuario) {
        return usuarioService.buscarUsuarioPorEmail(emailUsuario)
                .orElseThrow(() -> new RegraNegocioException("Não existe usuário cadastrado com este e-mail."));
    }

    private static String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
