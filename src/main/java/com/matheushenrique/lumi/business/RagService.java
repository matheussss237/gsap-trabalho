package com.matheushenrique.lumi.business;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matheushenrique.lumi.dto.ResultadoRagDTO;
import com.matheushenrique.lumi.dto.ResultadoRagDTO.DocumentoEncontrado;
import com.matheushenrique.lumi.exceptions.RegraNegocioException;
import com.matheushenrique.lumi.exceptions.ResourceNotFoundException;
import com.matheushenrique.lumi.infrastructure.entitys.Documento;
import com.matheushenrique.lumi.infrastructure.repository.DocumentoRepository;
import com.matheushenrique.lumi.rag.ProcessadorTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/*
 * RAG por palavras-chave + base de conhecimento.
 * Pergunta -> normalização -> tokenização -> stop words -> palavras-chave
 *          -> busca no MongoDB (documents) -> pontuação -> contexto enviado para a Groq
 */
@Service
@RequiredArgsConstructor
public class RagService {

    public static final int MAXIMO_DOCUMENTOS = 3;

    private final DocumentoRepository repository;
    private final UsuarioService usuarioService;

    public ResultadoRagDTO buscarContexto(String pergunta) {
        List<String> palavras = ProcessadorTexto.extrairPalavrasChave(pergunta);
        if (palavras.isEmpty()) {
            return new ResultadoRagDTO(palavras, List.of());
        }
        List<Documento> encontrados = repository.buscarPorPalavrasChave(palavras, usuarioService.getIdUsuarioLogado());
        return new ResultadoRagDTO(palavras, ordenarPorPontuacao(encontrados, palavras));
    }

    // cada palavra da pergunta vale: 3 pontos se é palavra-chave, 2 se está no título, 1 se está no conteúdo
    public static List<DocumentoEncontrado> ordenarPorPontuacao(List<Documento> documentos, List<String> palavras) {
        List<DocumentoEncontrado> resultado = new ArrayList<>();
        for (Documento documento : documentos) {
            Set<String> chaves = new HashSet<>();
            documento.getPalavrasChave().forEach(p -> chaves.addAll(ProcessadorTexto.extrairPalavrasChave(p)));
            List<String> titulo = ProcessadorTexto.extrairPalavrasChave(documento.getTitulo());
            List<String> conteudo = ProcessadorTexto.extrairPalavrasChave(documento.getConteudo());

            int pontos = 0;
            List<String> termos = new ArrayList<>();
            for (String palavra : palavras) {
                int valor = (chaves.contains(palavra) ? 3 : 0) + (titulo.contains(palavra) ? 2 : 0)
                        + (conteudo.contains(palavra) ? 1 : 0);
                if (valor > 0) {
                    pontos += valor;
                    termos.add(palavra);
                }
            }
            if (pontos > 0) {
                resultado.add(new DocumentoEncontrado(documento, pontos, termos));
            }
        }
        resultado.sort(Comparator.comparingInt(DocumentoEncontrado::pontuacao).reversed()
                .thenComparing(d -> d.documento().getTitulo()));
        return resultado.stream().limit(MAXIMO_DOCUMENTOS).toList();
    }

    // mensagem "system": personalidade da Lumi, nome do usuário e os documentos encontrados
    public String montarInstrucoes(ResultadoRagDTO resultado, String nomeUsuario) {
        StringBuilder texto = new StringBuilder();
        texto.append("Você é a Lumi, a assistente de IA pessoal de ")
                .append(nomeUsuario.isBlank() ? "um colaborador da empresa" : nomeUsuario).append(".\n")
                .append("Chame o usuário pelo nome quando fizer sentido e responda sempre em português do Brasil, ")
                .append("de forma clara e didática. Use Markdown simples (listas, **negrito** e blocos de código).\n");

        if (!resultado.temContexto()) {
            texto.append("\nNenhum documento da base de conhecimento tem relação com a pergunta; use seu conhecimento geral.");
            return texto.toString();
        }
        texto.append("\nUse como fonte principal os documentos da base de conhecimento abaixo. ")
                .append("Se a resposta não estiver neles, complete com seu conhecimento e avise.\n");
        int numero = 1;
        for (DocumentoEncontrado item : resultado.documentos()) {
            String conteudo = item.documento().getConteudo();
            texto.append("\n[Documento ").append(numero++).append("] ").append(item.documento().getTitulo()).append("\n")
                    .append(conteudo.length() > 2500 ? conteudo.substring(0, 2500) + "..." : conteudo).append("\n");
        }
        return texto.toString();
    }

    // ---------- base de conhecimento ----------

    public List<Documento> listarDocumentos() {
        return repository.listarVisiveis(usuarioService.getIdUsuarioLogado());
    }

    public Documento cadastrarDocumento(String titulo, String conteudo, String palavrasDigitadas) {
        String tituloValido = UsuarioService.obrigatorio(titulo, "Título");
        String conteudoValido = UsuarioService.obrigatorio(conteudo, "Conteúdo");
        if (conteudoValido.length() > 20000) {
            throw new RegraNegocioException("O conteúdo deve ter no máximo 20.000 caracteres.");
        }
        List<String> digitadas = palavrasDigitadas == null ? List.of() : Arrays.asList(palavrasDigitadas.split("[,;]"));

        Documento documento = Documento.builder()
                .donoId(usuarioService.getIdUsuarioLogado())
                .titulo(tituloValido)
                .conteudo(conteudoValido)
                .palavrasChave(gerarPalavrasChave(tituloValido, conteudoValido, digitadas))
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
        return repository.save(documento);
    }

    public void excluirDocumento(String id) {
        if (repository.deleteByIdAndDonoId(id, usuarioService.getIdUsuarioLogado()) == 0) {
            throw new ResourceNotFoundException("Você só pode excluir documentos que você cadastrou.");
        }
    }

    // primeira execução: cadastra os documentos de exemplo (resources/rag/seed-documents.json)
    public int cadastrarDocumentosIniciais() {
        if (repository.countByDonoIdIsNull() > 0) {
            return 0;
        }
        int total = 0;
        try (InputStreamReader leitor = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/rag/seed-documents.json")), StandardCharsets.UTF_8)) {
            for (JsonElement item : JsonParser.parseReader(leitor).getAsJsonArray()) {
                JsonObject json = item.getAsJsonObject();
                List<String> digitadas = new ArrayList<>();
                json.getAsJsonArray("keywords").forEach(p -> digitadas.add(p.getAsString()));
                String titulo = json.get("title").getAsString();
                String conteudo = json.get("content").getAsString();
                repository.save(Documento.builder()
                        .titulo(titulo)
                        .conteudo(conteudo)
                        .palavrasChave(gerarPalavrasChave(titulo, conteudo, digitadas))
                        .criadoEm(LocalDateTime.now())
                        .atualizadoEm(LocalDateTime.now())
                        .build());
                total++;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler a base de conhecimento inicial", e);
        }
        return total;
    }

    // palavras digitadas + palavras do título + as 40 que mais aparecem no conteúdo
    static List<String> gerarPalavrasChave(String titulo, String conteudo, List<String> digitadas) {
        Set<String> palavras = new LinkedHashSet<>();
        digitadas.forEach(p -> palavras.addAll(ProcessadorTexto.extrairPalavrasChave(p)));
        palavras.addAll(ProcessadorTexto.extrairPalavrasChave(titulo));
        palavras.addAll(ProcessadorTexto.palavrasMaisFrequentes(conteudo, 40));
        return new ArrayList<>(palavras);
    }
}
