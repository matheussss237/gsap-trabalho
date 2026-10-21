package com.matheushenrique.lumi.business;

import com.matheushenrique.lumi.dto.RespostaChatDTO;
import com.matheushenrique.lumi.dto.ResultadoRagDTO;
import com.matheushenrique.lumi.exceptions.RegraNegocioException;
import com.matheushenrique.lumi.infrastructure.entitys.Conversa;
import com.matheushenrique.lumi.infrastructure.entitys.Mensagem;
import com.matheushenrique.lumi.infrastructure.entitys.Usuario;
import com.matheushenrique.lumi.infrastructure.repository.MensagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ChatController -> ChatService -> RagService -> GroqService -> Groq API -> MongoDB -> tela
@Service
@RequiredArgsConstructor
public class ChatService {

    public static final int TAMANHO_MAXIMO = 4000;
    private static final int MEMORIA = 10; // quantas mensagens anteriores vão junto para a IA

    private final ConversaService conversaService;
    private final MensagemRepository mensagemRepository;
    private final RagService ragService;
    private final GroqService groqService;
    private final UsuarioService usuarioService;

    public RespostaChatDTO enviarMensagem(String conversaId, String texto) {
        Usuario usuario = usuarioService.getUsuarioLogado();
        String pergunta = texto == null ? "" : texto.trim();
        if (pergunta.isEmpty()) {
            throw new RegraNegocioException("Digite uma mensagem antes de enviar.");
        }
        if (pergunta.length() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("A mensagem deve ter no máximo " + TAMANHO_MAXIMO + " caracteres.");
        }

        Conversa conversa = conversaService.buscarConversaPorId(conversaId); // confere se é do usuário
        List<Mensagem> historico = conversaService.listarMensagens(conversa.getId());
        ResultadoRagDTO contexto = ragService.buscarContexto(pergunta);

        // monta o que vai para a IA: instruções + últimas mensagens + pergunta nova
        List<Mensagem> prompt = new ArrayList<>();
        prompt.add(Mensagem.builder().autor(Mensagem.Autor.SISTEMA)
                .conteudo(ragService.montarInstrucoes(contexto, usuario.primeiroNome())).build());
        prompt.addAll(historico.subList(Math.max(0, historico.size() - MEMORIA), historico.size()));
        prompt.add(Mensagem.builder().autor(Mensagem.Autor.USUARIO).conteudo(pergunta).build());

        // chama a IA antes de salvar: se der erro, nada fica pela metade no histórico
        String respostaIa = groqService.perguntar(prompt);

        LocalDateTime agora = LocalDateTime.now();
        Mensagem mensagemUsuario = mensagemRepository.save(Mensagem.builder()
                .conversaId(conversa.getId())
                .usuarioId(usuario.getId())
                .autor(Mensagem.Autor.USUARIO)
                .conteudo(pergunta)
                .criadoEm(agora)
                .build());
        Mensagem mensagemIa = mensagemRepository.save(Mensagem.builder()
                .conversaId(conversa.getId())
                .usuarioId(usuario.getId())
                .autor(Mensagem.Autor.ASSISTENTE)
                .conteudo(respostaIa)
                .fontes(contexto.titulos())
                .criadoEm(agora.plusNanos(1_000_000))
                .build());

        // a primeira pergunta vira o título da conversa
        if (Conversa.TITULO_PADRAO.equals(conversa.getTitulo())) {
            conversa = conversaService.renomearConversa(conversa.getId(), tituloAutomatico(pergunta));
        } else {
            conversa = conversaService.atualizarData(conversa);
        }
        return new RespostaChatDTO(mensagemUsuario, mensagemIa, conversa);
    }

    static String tituloAutomatico(String pergunta) {
        String linha = pergunta.replaceAll("\\s+", " ").trim();
        if (linha.length() <= 40) {
            return linha;
        }
        int corte = linha.lastIndexOf(' ', 40);
        return linha.substring(0, corte > 15 ? corte : 40) + "…";
    }
}
