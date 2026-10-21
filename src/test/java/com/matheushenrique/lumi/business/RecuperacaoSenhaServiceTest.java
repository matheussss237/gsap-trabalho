package com.matheushenrique.lumi.business;

import com.matheushenrique.lumi.exceptions.RegraNegocioException;
import com.matheushenrique.lumi.infrastructure.repository.TokenRecuperacaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RecuperacaoSenhaServiceTest extends TesteBase {

    @Autowired TokenRecuperacaoRepository tokenRepository;

    @BeforeEach
    void preparar() {
        cadastrar("Usuário Um", "usuario1@teste.com");
    }

    @AfterEach
    void voltarRelogio() {
        recuperacaoSenhaService.setRelogio(Clock.systemDefaultZone());
    }

    private String pedirCodigo() {
        return recuperacaoSenhaService.solicitarCodigo("usuario1@teste.com").orElseThrow();
    }

    @Test
    void fluxoCompletoEOCodigoSoServeUmaVez() {
        String codigo = pedirCodigo();
        assertNotEquals(codigo, tokenRepository.findAll().get(0).getCodigoHash(), "só o hash fica salvo");

        recuperacaoSenhaService.redefinirSenha("usuario1@teste.com", codigo, "senhaNova123", "senhaNova123");
        assertDoesNotThrow(() -> usuarioService.fazerLogin("usuario1@teste.com", "senhaNova123"));
        assertThrows(RegraNegocioException.class,
                () -> recuperacaoSenhaService.redefinirSenha("usuario1@teste.com", codigo, "outra12345", "outra12345"));
    }

    @Test
    void codigoVencido() {
        String codigo = pedirCodigo();
        recuperacaoSenhaService.setRelogio(Clock.offset(Clock.systemDefaultZone(), Duration.ofMinutes(16)));
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> recuperacaoSenhaService.redefinirSenha("usuario1@teste.com", codigo, "senhaNova123", "senhaNova123"));
        assertTrue(erro.getMessage().contains("expirou"));
    }

    @Test
    void cincoTentativasErradasBloqueiam() {
        String codigo = pedirCodigo();
        String errado = codigo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < RecuperacaoSenhaService.MAXIMO_TENTATIVAS; i++) {
            assertThrows(RegraNegocioException.class,
                    () -> recuperacaoSenhaService.redefinirSenha("usuario1@teste.com", errado, "senhaNova123", "senhaNova123"));
        }
        assertThrows(RegraNegocioException.class,
                () -> recuperacaoSenhaService.redefinirSenha("usuario1@teste.com", codigo, "senhaNova123", "senhaNova123"));
    }

    @Test
    void codigoNovoCancelaOAnterior() {
        String primeiro = pedirCodigo();
        String segundo = pedirCodigo();
        if (!primeiro.equals(segundo)) {
            assertThrows(RegraNegocioException.class,
                    () -> recuperacaoSenhaService.redefinirSenha("usuario1@teste.com", primeiro, "senhaNova123", "senhaNova123"));
        }
        assertDoesNotThrow(
                () -> recuperacaoSenhaService.redefinirSenha("usuario1@teste.com", segundo, "senhaNova123", "senhaNova123"));
    }

    @Test
    void emailInexistente() {
        assertThrows(RegraNegocioException.class, () -> recuperacaoSenhaService.solicitarCodigo("ninguem@teste.com"));
    }
}
