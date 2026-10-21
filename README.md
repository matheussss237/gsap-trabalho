# Lumi

**Lumi** é uma aplicação desktop de chat com Inteligência Artificial feita com **Java 21, JavaFX 21,
Spring Boot, MongoDB Atlas, RAG por palavras-chave e API da Groq**. A animação de entrada é feita com
**GSAP** dentro de um `WebView`.

<p align="center">
  <img src="docs/telas/01-splash.png" width="560" alt="Animação de entrada (GSAP)">
</p>

| Login (claro) | Chat (escuro) |
|---|---|
| ![Login](docs/telas/03-login-claro.png) | ![Chat](docs/telas/07-chat-escuro.png) |

| Chat (claro) | Base de conhecimento → "Testar busca (RAG)" |
|---|---|
| ![Chat claro](docs/telas/08-chat-claro.png) | ![RAG](docs/telas/11-teste-rag.png) |

> As imagens de `docs/telas/` foram geradas com um MongoDB e uma Groq de teste. As evidências com o
> MongoDB Atlas e a Groq reais ficam em `docs/evidencias/` (veja `docs/TESTES.md`).

---

## 1. Sobre o projeto

A Lumi é uma assistente de IA para os colaboradores de uma empresa. Cada pessoa tem a sua conta e as
suas próprias conversas. As perguntas vão para a Groq junto com trechos da **base de conhecimento**
guardada no MongoDB (RAG), e todo o histórico fica salvo no MongoDB Atlas.

O que a Lumi faz:

- animação de entrada com **GSAP** (`gsap.timeline`, `from`, `to`, `fromTo`, `stagger`) dentro de um `WebView`;
- cadastro, login, **sessão** e logout;
- recuperação de senha com **código temporário** (vale 15 minutos, só pode ser usado uma vez, aceita até 5 tentativas);
- conversas **separadas por usuário**: criar, listar, buscar, renomear e excluir (com confirmação);
- envio de mensagens para a **Groq**, que recebe junto as últimas mensagens da conversa;
- **IA personalizada**: a Lumi recebe o nome de quem está logado e responde chamando a pessoa pelo nome;
- **RAG** por palavras-chave com remoção de **stop words**, e cadastro de documentos na base de conhecimento;
- tema **claro e escuro** com JavaFX CSS (a escolha fica salva);
- respostas em Markdown (títulos, listas, negrito e blocos de código com botão "Copiar");
- erros explicados com mensagens que o usuário entende;
- instalador para Windows gerado com **jpackage**.

### Diferencial

1. **RAG transparente**: cada resposta mostra as **fontes**, ou seja, os documentos usados como
   contexto. A janela *Base de conhecimento → Testar busca* mostra cada etapa da busca: palavras-chave,
   stop words removidas e a pontuação de cada documento encontrado.
2. **Splash ligada ao carregamento real**: enquanto o GSAP anima, o Java liga o Spring Boot e conecta
   no MongoDB, mandando o andamento para a animação (`setStatus`). O GSAP avisa o Java quando a entrada
   terminou (`javaBridge.introDone`) e quando a saída acabou (`javaBridge.finish`).
3. **Base com documentos da empresa** (home office, suporte técnico, reembolso). São informações que o
   modelo não conhece sozinho, então dá para ver claramente o efeito do RAG.

## 2. Tecnologias

| Tecnologia | Para que serve |
|---|---|
| Java 21 | linguagem |
| JavaFX 21 (`javafx-controls`, `javafx-web`) | telas feitas só com código (sem Scene Builder) e o `WebView` da splash |
| JavaFX CSS | visual e temas (`base.css`, `light.css`, `dark.css`) |
| GSAP 3.13 (guardado em `resources/splash`) | animação de entrada |
| Spring Boot 3.3 | injeção de dependências (`@Service`) e `application.properties` |
| Spring Data MongoDB (`MongoRepository`) + MongoDB Atlas | banco de dados |
| Lombok | `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor` |
| spring-security-crypto (`BCryptPasswordEncoder`) | hash das senhas |
| Groq API (`/openai/v1/chat/completions`) + `java.net.http` + Gson | IA |
| Spring Mail | envio do código de recuperação por e-mail (opcional) |
| JUnit 5 + mongo-java-server | testes com um MongoDB falso em memória |
| Maven, IntelliJ IDEA, Git/GitHub, jpackage + WiX | build, IDE, versionamento e instalador |

## 3. Arquitetura

O projeto segue a arquitetura pedida, usando os nomes de pacote dos meus projetos Spring (a pasta
`business` é a camada **Service**):

```
View  →  Controller  →  Service (business)  →  Repository  →  MongoDB
```

- **view**: monta as telas (`LoginView`, `ChatView`, `BaseConhecimentoView`…).
- **controller**: recebe os cliques e chama os services. Nunca acessa o banco.
- **business**: regras e validações (`UsuarioService`, `ChatService`, `RagService`…), todas com `@Service`.
- **infrastructure/repository**: interfaces `MongoRepository`; o Spring Data cria as consultas pelo nome
  do método (`findByIdAndUsuarioId`, `deleteByConversaIdAndUsuarioId`…).
- **infrastructure/entitys**: classes salvas no MongoDB (`@Document`).

Caminho de uma pergunta:

```
ChatView → ChatController → ChatService → RagService  (busca na coleção documents)
                                        → GroqService → Groq API
                                        → MensagemRepository (salva pergunta e resposta) → MongoDB
         ← a resposta aparece na tela
```

As operações demoradas (MongoDB e Groq) rodam fora da thread do JavaFX (`Componentes.emSegundoPlano`),
então a tela não trava. Enquanto isso aparecem indicadores: botão "carregando" e "A Lumi está
consultando a base de conhecimento…".

Inicialização:

```
LumiApplication.main → Main (JavaFX) → SplashView → WebView → index.html + splash.js → GSAP
                         │                                      ▲ setStatus      │ introDone / finish
                         │  ao mesmo tempo: liga o Spring Boot, │                ▼
                         │  testa o MongoDB e cadastra a base ──┘         tela de login
```

## 4. Estrutura de pastas

```
src/main/java/com/matheushenrique/lumi/
├── LumiApplication.java   classe principal (@SpringBootApplication) - rode esta
├── Main.java              parte JavaFX: splash, liga o Spring e troca as telas
├── config/                MongoConfig (tempo limite da conexão)
├── controller/            LoginController (login, cadastro, recuperar senha), ChatController, TemaController
├── business/              UsuarioService, RecuperacaoSenhaService, ConversaService, ChatService,
│                          RagService, GroqService
├── rag/                   ProcessadorTexto (normalização, tokenização, stop words, palavras-chave)
├── dto/                   CadastroDTO, RespostaChatDTO, ResultadoRagDTO (records)
├── exceptions/            RegraNegocioException, ResourceNotFoundException, GlobalExceptionHandler
├── infrastructure/
│   ├── entitys/           Usuario, Conversa, Mensagem, Documento, TokenRecuperacao
│   └── repository/        UsuarioRepository, ConversaRepository, MensagemRepository,
│                          DocumentoRepository, TokenRecuperacaoRepository
└── view/                  SplashView, LoginView, ChatView, MensagemView, BaseConhecimentoView, Componentes

src/main/resources/
├── application.properties configurações (lê MONGODB_URI e GROQ_API_KEY das variáveis de ambiente)
├── css/                   base.css, light.css, dark.css
├── images/                ícones
├── rag/                   stopwords-pt.txt, seed-documents.json (base de conhecimento inicial)
└── splash/                index.html, splash.css, splash.js, gsap.min.js

src/test/java/             testes JUnit + PreviewTelas (gera prints das telas)
.run/                      configurações do IntelliJ ("Lumi" e "Testes")
packaging/  scripts/  docs/
```

## 5. Pré-requisitos

- **IntelliJ IDEA** (a versão gratuita serve) com o plugin Lombok (já vem instalado)
- **JDK 21**: o IntelliJ consegue baixar (*Download JDK → Eclipse Temurin 21*)
- Conta no **MongoDB Atlas** (o cluster gratuito M0 serve)
- Conta na **Groq** (https://console.groq.com) para criar a API Key
- Para o instalador: Windows 10/11 + **WiX Toolset 3.x**

## 6. Configuração

Nenhuma senha ou chave fica no código. O `application.properties` lê tudo de variáveis de ambiente:

```properties
spring.data.mongodb.uri=${MONGODB_URI:}
groq.api.key=${GROQ_API_KEY:}
```

| Variável | Obrigatória | Para que serve |
|---|---|---|
| `MONGODB_URI` | sim | endereço do Atlas (`mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/...`) |
| `GROQ_API_KEY` | sim (para conversar) | chave da Groq (`gsk_...`) |
| `MONGODB_DATABASE` | não | nome do banco (padrão `lumi`) |
| `GROQ_MODEL` | não | modelo (padrão `llama-3.3-70b-versatile`) |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | não | enviar o código de recuperação por e-mail |

### MongoDB Atlas

1. Crie um cluster (M0).
2. Em *Database Access*, crie um usuário com senha (use só letras e números).
3. Em *Network Access*, clique em *Add Current IP Address*.
4. Em *Connect → Drivers → Java*, copie o endereço e troque `<db_password>` pela senha.

### Groq

Em https://console.groq.com/keys, clique em *Create API Key* e copie a chave.

### Onde colocar as variáveis (escolha uma opção)

- **IntelliJ:** *Run → Edit Configurations → Lumi → Environment variables*:
  `MONGODB_URI=...;GROQ_API_KEY=...`
- **Windows (vale também para o app instalado).** No PowerShell, rode os comandos abaixo e depois
  feche e abra o IntelliJ:
  ```powershell
  setx MONGODB_URI "mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/"
  setx GROQ_API_KEY "gsk_sua_chave"
  ```
- **Arquivo:** copie `config.properties.example` para `%USERPROFILE%\.lumi\config.properties` e
  preencha. Esse arquivo fica fora do projeto e está no `.gitignore`.

## 7. Banco de dados

Banco `lumi`. Os índices são criados sozinhos pelo Spring (`auto-index-creation=true`).

| Coleção | Classe | Campos | Índices |
|---|---|---|---|
| `users` | `Usuario` | `id`, `nome`, `email`, `senha` (hash BCrypt), `criadoEm`, `atualizadoEm` | `email` **único** |
| `conversations` | `Conversa` | `id`, `usuarioId` (dono), `titulo`, `criadoEm`, `atualizadoEm` | `usuarioId + atualizadoEm` |
| `messages` | `Mensagem` | `id`, `conversaId`, `usuarioId`, `autor` (`USUARIO`/`ASSISTENTE`), `conteudo`, `fontes`, `criadoEm` | `conversaId + usuarioId + criadoEm` |
| `documents` | `Documento` | `id`, `donoId` (`null` = público), `titulo`, `conteudo`, `palavrasChave`, `criadoEm`, `atualizadoEm` | `palavrasChave` |
| `password_reset_tokens` | `TokenRecuperacao` | `id`, `usuarioId`, `codigoHash` (SHA-256), `criadoEm`, `expiraEm`, `usado`, `tentativas` | `usuarioId`, **TTL** em `expiraEm` |

Na primeira execução a Lumi cadastra os 12 documentos de `rag/seed-documents.json` (Java, JavaFX,
MongoDB, MVC, BCrypt, RAG, Groq e políticas fictícias da empresa).

## 8. RAG

O código está em `rag/ProcessadorTexto.java` e `business/RagService.java`:

```
Pergunta: "Qual é a política de home office da empresa?"
  1. normalizar       → "qual e a politica de home office da empresa"  (minúsculas, sem acento e pontuação)
  2. tokenizar        → [qual, e, a, politica, de, home, office, da, empresa]
  3. stop words       → tira [qual, e, a, de, da]
  4. palavras-chave   → [politica, home, office, empresa]   (+ plural simples: "padrões" → "padrao")
  5. busca no MongoDB → DocumentoRepository.buscarPorPalavrasChave (documentos públicos + os do usuário)
  6. pontuação        → por palavra: 3 pontos se é palavra-chave, 2 se está no título, 1 se está no conteúdo
  7. contexto         → os 3 melhores documentos vão na mensagem "system" para a Groq
```

### Por que remover stop words?

Stop words são artigos, preposições, conjunções, pronomes e verbos de pedido, como "a", "de", "para",
"que" e "explique". Elas aparecem em quase **todas** as frases, então não ajudam a descobrir **sobre o
que** é a pergunta. Se ficassem, quase todo documento "combinaria" com qualquer pergunta (todos têm
"de" e "para"), a pontuação ficaria errada e a IA receberia um contexto que não tem nada a ver. Sem
elas, sobram só as palavras com significado ("política", "home office", "suporte"), e a busca fica mais
certeira. A lista está em `src/main/resources/rag/stopwords-pt.txt`.

## 9. Como rodar no IntelliJ

1. Abra a pasta do projeto (a que tem o `pom.xml`) e clique em **Load Maven Project** se aparecer o aviso.
2. Em *File → Project Structure → SDK*, escolha o **JDK 21**.
3. Configure as variáveis (seção 6).
4. No topo, escolha a configuração **Lumi** e clique em ▶. Ela roda a classe `LumiApplication`.
5. A configuração **Testes** roda os 22 testes.

Pelo terminal: `mvn spring-boot:run` (abre o app) e `mvn test` (testes).

## 10. Instalação (versão distribuível)

### Gerar o instalador (Windows)

1. Instale o **WiX Toolset 3.14** e coloque a pasta `bin` dele no `PATH`.
2. No terminal do IntelliJ, na pasta do projeto:

```bat
scripts\build-installer.bat           :: gera target\installer\Lumi-1.0.0.exe
scripts\build-installer.bat app-image :: pasta com o executável (não precisa do WiX)
```

O script roda `mvn clean package`, que gera `target/dist/lumi.jar` e as bibliotecas em
`target/dist/libs/`, e depois o `jpackage`. O `jpackage` coloca junto um Java reduzido, então **o
computador de destino não precisa ter Java**. O instalador tem nome, versão e ícone, deixa escolher a
pasta e cria atalho na área de trabalho e no menu Iniciar.

### Instalar em outro computador

1. Execute `Lumi-1.0.0.exe`.
2. Configure `MONGODB_URI` e `GROQ_API_KEY` (com `setx` ou com `%USERPROFILE%\.lumi\config.properties`).
3. Libere o IP desse computador no Atlas (*Network Access*).
4. Abra a **Lumi** pelo menu Iniciar.

## 11. Segurança

- **Senhas:** `BCryptPasswordEncoder` (salt aleatório). A senha nunca é salva nem mostrada. É exigido
  no mínimo 8 caracteres, com letras e números.
- **Recuperar senha:** a senha antiga não pode ser recuperada. É gerado um código de 6 dígitos
  (`SecureRandom`), e no banco fica só o **SHA-256** dele. O código vale **15 minutos**, só pode ser
  usado **uma vez** e é cancelado depois de **5 tentativas erradas** ou quando um código novo é pedido.
  O MongoDB apaga os códigos vencidos sozinho (índice TTL). Sem e-mail configurado, a Lumi fica em
  *modo demonstração* e mostra o código na tela (ver Limitações).
- **Chaves e senha do banco:** vêm de variáveis de ambiente (`${MONGODB_URI}`, `${GROQ_API_KEY}`) ou de
  um arquivo fora do projeto. O `.gitignore` bloqueia `.env` e `config.properties`.
- **Conexão segura:** `mongodb+srv://` usa TLS.
- **Sessão:** o `UsuarioService` guarda quem está logado. Os outros services pegam o dono dos dados
  sempre de lá, e não da tela. O logout limpa a sessão e volta para o login.
- **Isolamento:** os repositories buscam, alteram e excluem sempre por **id + usuarioId**
  (`findByIdAndUsuarioId`, `deleteByConversaIdAndUsuarioId`), então uma conversa de outro usuário
  simplesmente "não existe". Isso é comprovado no `ConversaServiceTest`.
- **Erros:** o `GlobalExceptionHandler` transforma qualquer erro numa mensagem que o usuário entende;
  detalhes técnicos só vão para o console.

## 12. Limitações

- O RAG é por **palavras-chave**, sem sinônimos: se a pergunta usar palavras diferentes das do
  documento, ele não é encontrado. A regra de plural é simples e pode errar em alguns casos.
- Sem e-mail configurado, a recuperação de senha fica em **modo demonstração** (o código aparece na tela).
- O login avisa se o problema é "usuário inexistente" ou "senha incorreta" (o enunciado testa os dois
  casos), o que permite descobrir se um e-mail está cadastrado. Também não há bloqueio depois de muitas
  tentativas de login.
- A sessão fica só na memória: ao fechar a Lumi, é preciso entrar de novo.
- A resposta da IA aparece inteira (não vai aparecendo aos poucos), e só as 10 últimas mensagens vão
  como memória.
- O `.exe` precisa ser gerado no Windows com WiX 3.x.

## 13. Testes

`mvn test` (ou a configuração **Testes**) roda 22 testes que sobem o Spring de verdade com um
**MongoDB falso em memória** (mongo-java-server) e uma IA falsa. Assim os repositories e as consultas
são testados sem precisar do Atlas nem de internet. O roteiro dos testes manuais está em
[`docs/TESTES.md`](docs/TESTES.md).

`scripts/preview-ui.sh` (Linux + Xvfb) roda o app inteiro com MongoDB e Groq falsos e salva prints em
`target/preview/`.
