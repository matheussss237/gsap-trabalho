package com.matheushenrique.lumi.business;

import com.matheushenrique.lumi.dto.CadastroDTO;
import com.matheushenrique.lumi.exceptions.RegraNegocioException;
import com.matheushenrique.lumi.infrastructure.entitys.Usuario;
import com.matheushenrique.lumi.infrastructure.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioServiceTest extends TesteBase {

    @Autowired UsuarioRepository usuarioRepository;

    @Test
    void cadastroValidoSalvaHashENaoASenha() {
        Usuario usuario = cadastrar("Usuário Um", "Usuario1@Teste.com");
        Usuario salvo = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertEquals("usuario1@teste.com", salvo.getEmail());
        assertNotEquals("senha1234", salvo.getSenha());
        assertTrue(salvo.getSenha().startsWith("$2a$"));
    }

    @Test
    void cadastroDuplicado() {
        cadastrar("Usuário Um", "usuario1@teste.com");
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> cadastrar("Outro", "USUARIO1@teste.com"));
        assertTrue(erro.getMessage().contains("Já existe"));
    }

    @Test
    void cadastroComCamposInvalidos() {
        assertThrows(RegraNegocioException.class,
                () -> usuarioService.cadastrarUsuario(new CadastroDTO("", "a@b.com", "senha1234", "senha1234")));
        assertThrows(RegraNegocioException.class,
                () -> usuarioService.cadastrarUsuario(new CadastroDTO("Ana", "email-errado", "senha1234", "senha1234")));
        assertThrows(RegraNegocioException.class,
                () -> usuarioService.cadastrarUsuario(new CadastroDTO("Ana", "a@b.com", "curta1", "curta1")));
        assertThrows(RegraNegocioException.class,
                () -> usuarioService.cadastrarUsuario(new CadastroDTO("Ana", "a@b.com", "senha1234", "outra12345")));
    }

    @Test
    void senhaIncorreta() {
        cadastrar("Usuário Um", "usuario1@teste.com");
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> usuarioService.fazerLogin("usuario1@teste.com", "errada123"));
        assertTrue(erro.getMessage().contains("Senha incorreta"));
        assertFalse(usuarioService.estaLogado());
    }

    @Test
    void usuarioInexistente() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> usuarioService.fazerLogin("ninguem@teste.com", "senha1234"));
        assertTrue(erro.getMessage().contains("Não existe usuário"));
    }

    @Test
    void loginComecaSessaoELogoutTermina() {
        Usuario usuario = cadastrar("Usuário Um", "usuario1@teste.com");
        entrar("usuario1@teste.com");
        assertEquals(usuario.getId(), usuarioService.getIdUsuarioLogado());

        usuarioService.fazerLogout();
        assertFalse(usuarioService.estaLogado());
        assertThrows(RegraNegocioException.class, () -> conversaService.listarConversas());
    }
}
