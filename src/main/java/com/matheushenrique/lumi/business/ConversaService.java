package com.matheushenrique.lumi.business;

import com.matheushenrique.lumi.exceptions.RegraNegocioException;
import com.matheushenrique.lumi.exceptions.ResourceNotFoundException;
import com.matheushenrique.lumi.infrastructure.entitys.Conversa;
import com.matheushenrique.lumi.infrastructure.entitys.Mensagem;
import com.matheushenrique.lumi.infrastructure.repository.ConversaRepository;
import com.matheushenrique.lumi.infrastructure.repository.MensagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// criar, listar, renomear e excluir conversas - sempre só as do usuário logado
@Service
@RequiredArgsConstructor
public class ConversaService {

    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;
    private final UsuarioService usuarioService;

    public Conversa criarConversa() {
        Conversa conversa = Conversa.builder()
                .usuarioId(usuarioService.getIdUsuarioLogado())
                .titulo(Conversa.TITULO_PADRAO)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
        return conversaRepository.save(conversa);
    }

    public List<Conversa> listarConversas() {
        return conversaRepository.findByUsuarioIdOrderByAtualizadoEmDesc(usuarioService.getIdUsuarioLogado());
    }

    public Conversa buscarConversaPorId(String id) {
        return conversaRepository.findByIdAndUsuarioId(id, usuarioService.getIdUsuarioLogado())
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada. Ela pode ter sido excluída."));
    }

    public List<Mensagem> listarMensagens(String conversaId) {
        Conversa conversa = buscarConversaPorId(conversaId);
        return mensagemRepository.findByConversaIdAndUsuarioIdOrderByCriadoEmAsc(conversa.getId(), conversa.getUsuarioId());
    }

    public Conversa renomearConversa(String id, String novoTitulo) {
        String titulo = UsuarioService.obrigatorio(novoTitulo, "Título");
        if (titulo.length() > 80) {
            throw new RegraNegocioException("O título deve ter no máximo 80 caracteres.");
        }
        Conversa conversa = buscarConversaPorId(id);
        conversa.setTitulo(titulo);
        conversa.setAtualizadoEm(LocalDateTime.now());
        return conversaRepository.save(conversa);
    }

    public void excluirConversa(String id) {
        Conversa conversa = buscarConversaPorId(id);
        mensagemRepository.deleteByConversaIdAndUsuarioId(conversa.getId(), conversa.getUsuarioId());
        conversaRepository.deleteByIdAndUsuarioId(conversa.getId(), conversa.getUsuarioId());
    }

    // a conversa usada agora sobe para o topo da lista
    public Conversa atualizarData(Conversa conversa) {
        conversa.setAtualizadoEm(LocalDateTime.now());
        return conversaRepository.save(conversa);
    }
}
