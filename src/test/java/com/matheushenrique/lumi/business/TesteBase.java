package com.matheushenrique.lumi.business;

import com.matheushenrique.lumi.dto.CadastroDTO;
import com.matheushenrique.lumi.exceptions.RegraNegocioException;
import com.matheushenrique.lumi.infrastructure.entitys.Mensagem;
import com.matheushenrique.lumi.infrastructure.entitys.Usuario;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

// base dos testes: sobe o Spring de verdade com um MongoDB falso em memória e uma IA falsa
@SpringBootTest
@Import(TesteBase.Configuracao.class)
abstract class TesteBase {

    static final MongoServer MONGO = new MongoServer(new MemoryBackend());
    static final InetSocketAddress ENDERECO = MONGO.bind();

    @DynamicPropertySource
    static void apontarParaMongoFalso(DynamicPropertyRegistry propriedades) {
        propriedades.add("spring.data.mongodb.uri",
                () -> "mongodb://" + ENDERECO.getHostString() + ":" + ENDERECO.getPort() + "/lumi-teste");
    }

    @TestConfiguration
    static class Configuracao {
        @Bean
        @Primary
        GroqFalso groqFalso() {
            return new GroqFalso();
        }
    }

    // finge ser a Groq: devolve uma resposta fixa e guarda o que recebeu
    static class GroqFalso extends GroqService {
        final List<List<Mensagem>> perguntas = new ArrayList<>();
        boolean falhar;
        String resposta = "Resposta de teste.";

        @Override
        public String perguntar(List<Mensagem> mensagens) {
            perguntas.add(new ArrayList<>(mensagens));
            if (falhar) throw new RegraNegocioException("A API da Groq está fora do ar agora.");
            return resposta;
        }

        List<Mensagem> ultimaPergunta() {
            return perguntas.get(perguntas.size() - 1);
        }
    }

    @Autowired UsuarioService usuarioService;
    @Autowired ConversaService conversaService;
    @Autowired ChatService chatService;
    @Autowired RagService ragService;
    @Autowired RecuperacaoSenhaService recuperacaoSenhaService;
    @Autowired GroqFalso groq;
    @Autowired MongoTemplate mongo;

    @BeforeEach
    void limparBanco() {
        mongo.getDb().drop();
        usuarioService.fazerLogout();
        groq.perguntas.clear();
        groq.falhar = false;
    }

    Usuario cadastrar(String nome, String email) {
        return usuarioService.cadastrarUsuario(new CadastroDTO(nome, email, "senha1234", "senha1234"));
    }

    Usuario entrar(String email) {
        return usuarioService.fazerLogin(email, "senha1234");
    }
}
