package com.matheushenrique.lumi.business;

import com.matheushenrique.lumi.exceptions.ResourceNotFoundException;
import com.matheushenrique.lumi.infrastructure.entitys.Conversa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// conversas: criar, renomear, excluir e o isolamento usuario1 ≠ usuario2
class ConversaServiceTest extends TesteBase {

    @BeforeEach
    void criarUsuarios() {
        cadastrar("Usuário Um", "usuario1@teste.com");
        cadastrar("Usuário Dois", "usuario2@teste.com");
    }

    @Test
    void cadaUsuarioSoVeAsProprias() {
        entrar("usuario1@teste.com");
        Conversa java = conversaService.renomearConversa(conversaService.criarConversa().getId(), "Java");
        conversaService.renomearConversa(conversaService.criarConversa().getId(), "JavaFX");
        usuarioService.fazerLogout();

        entrar("usuario2@teste.com");
        conversaService.renomearConversa(conversaService.criarConversa().getId(), "Angular");
        assertEquals(List.of("Angular"), titulos());
        // o usuario2 não consegue abrir, renomear nem excluir a conversa do usuario1
        assertThrows(ResourceNotFoundException.class, () -> conversaService.buscarConversaPorId(java.getId()));
        assertThrows(ResourceNotFoundException.class, () -> conversaService.listarMensagens(java.getId()));
        assertThrows(ResourceNotFoundException.class, () -> conversaService.renomearConversa(java.getId(), "Invadido"));
        assertThrows(ResourceNotFoundException.class, () -> conversaService.excluirConversa(java.getId()));
        usuarioService.fazerLogout();

        entrar("usuario1@teste.com");
        assertEquals(List.of("Java", "JavaFX"), titulos().stream().sorted().toList());
    }

    @Test
    void criarRenomearExcluirERecarregar() {
        entrar("usuario1@teste.com");
        Conversa nova = conversaService.criarConversa();
        assertEquals(Conversa.TITULO_PADRAO, nova.getTitulo());
        assertEquals(usuarioService.getIdUsuarioLogado(), nova.getUsuarioId());

        conversaService.renomearConversa(nova.getId(), "Estudando JavaFX");
        // "reiniciar o app": busca de novo no banco
        assertEquals("Estudando JavaFX", conversaService.buscarConversaPorId(nova.getId()).getTitulo());

        conversaService.excluirConversa(nova.getId());
        assertTrue(conversaService.listarConversas().isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> conversaService.excluirConversa(nova.getId()));
    }

    @Test
    void conversaInexistente() {
        entrar("usuario1@teste.com");
        assertThrows(ResourceNotFoundException.class, () -> conversaService.buscarConversaPorId("000000000000000000000000"));
        assertThrows(ResourceNotFoundException.class, () -> conversaService.buscarConversaPorId("id-errado"));
    }

    private List<String> titulos() {
        return conversaService.listarConversas().stream().map(Conversa::getTitulo).toList();
    }
}
