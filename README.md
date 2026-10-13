# Nexus Chat

Aplicação desktop de chat com Inteligência Artificial construída com **Java 21, JavaFX 21, MongoDB Atlas, RAG por palavras-chave e API da Groq**, com animação de entrada feita em **GSAP** dentro de um `WebView`.

<p align="center">
  <img src="docs/telas/01-splash-final.png" width="560" alt="Animação de entrada (GSAP)">
</p>

| Login (escuro) | Chat (claro) |
|---|---|
| ![Login](docs/telas/02-login-escuro.png) | ![Chat](docs/telas/08-chat-claro.png) |

| Chat (escuro) | Painel "Testar recuperação (RAG)" |
|---|---|
| ![Chat escuro](docs/telas/07-chat-escuro.png) | ![RAG](docs/telas/11-teste-rag.png) |

> As imagens de `docs/telas/` foram geradas com dados de demonstração. As evidências com o
> MongoDB Atlas e a Groq reais ficam em `docs/evidencias/` (veja `docs/TESTES.md`).

---

## 1. Sobre o projeto

O Nexus Chat é um assistente de IA para colaboradores de uma empresa. Cada usuário tem sua conta e
suas próprias conversas; as perguntas são enviadas à Groq junto com trechos relevantes de uma
**base de conhecimento** armazenada no MongoDB (RAG), e todo o histórico fica salvo no MongoDB Atlas.

Funcionalidades:

- animação de entrada com **GSAP** (`gsap.timeline`, `from`, `to`, `fromTo`, `stagger`) em `WebView`;
- cadastro, login, **sessão** e logout;
- recuperação de senha com **código temporário** (15 min, uso único, 5 tentativas);
- conversas **isoladas por usuário**: criar, listar, buscar, renomear e excluir (com confirmação);
- envio de mensagens para a **Groq**, com memória das últimas mensagens da conversa;
- **RAG** por palavras-chave com remoção de **stop words**;
- base de conhecimento com cadastro de documentos;
- tema **claro e escuro** com JavaFX CSS (preferência salva);
- respostas renderizadas com Markdown (títulos, listas, negrito, blocos de código com "Copiar");
- tratamento de erros com mensagens compreensíveis;
- instalador Windows gerado com **jpackage**.

### Diferencial

1. **RAG transparente** — cada resposta mostra as **fontes** (documentos da base usados como contexto) e
   a janela *Base de conhecimento → Testar recuperação* exibe cada etapa do RAG: palavras-chave
   extraídas, stop words removidas, documentos encontrados e a pontuação de cada um.
2. **Splash integrada ao carregamento real** — a animação GSAP conversa com o Java nos dois sentidos:
   o Java envia o status da conexão com o MongoDB para a página (`setStatus`) e o GSAP avisa o Java
   quando a entrada terminou (`javaBridge.introDone`) e quando a saída acabou (`javaBridge.finish`).
   A aplicação só sai da splash quando a animação terminou **e** o banco respondeu.
3. **Base de conhecimento com documentos da empresa** (políticas internas, suporte, reembolso) —
   informações que o modelo não conhece sozinho, o que deixa claro o efeito do RAG na apresentação.

## 2. Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | linguagem |
| JavaFX 21 (`javafx-controls`, `javafx-web`) | interface gráfica (100% em código, sem Scene Builder) e `WebView` da splash |
| JavaFX CSS | estilização e temas (`base.css`, `light.css`, `dark.css`) |
| GSAP 3.13 (embutido em `resources/splash`) | animação de entrada |
| Maven | build e dependências (`pom.xml`) |
| MongoDB Atlas + `mongodb-driver-sync` 5 | persistência |
| Groq API (`/openai/v1/chat/completions`) via `java.net.http.HttpClient` | IA |
| Gson | JSON da API da Groq |
| jBCrypt | hash de senhas |
| Angus Mail (Jakarta Mail) | envio opcional do código de recuperação por e-mail |
| JUnit 5 | testes |
| jpackage + WiX Toolset | instalador Windows |
| Git / GitHub | versionamento |

## 3. Arquitetura

```
View  →  Controller  →  Service  →  Repository  →  MongoDB
```

- **View** só monta a interface e expõe os componentes (`LoginView`, `ChatView`, `SidebarView`...).
- **Controller** trata eventos da View e chama os Services; não acessa o banco (`LoginController`, `ChatController`...).
- **Service** contém as regras de negócio e validações (`AuthenticationService`, `ChatService`, `RAGService`...).
- **Repository** é a única camada que fala com o MongoDB. São **interfaces** (`UserRepository`...) com
  implementação MongoDB (`MongoUserRepository`...). Nos testes são usadas implementações em memória.

Fluxo de uma pergunta:

```
ChatView → ChatController → ChatService → RAGService (busca na coleção documents)
                                        → GroqService → Groq API
                                        → MessageRepository (salva pergunta e resposta) → MongoDB
         ← resposta exibida no JavaFX
```

Operações demoradas (MongoDB e Groq) rodam fora da *JavaFX Application Thread* com `javafx.concurrent.Task`
(`util/FxAsync`), então a interface não congela; enquanto isso aparecem indicadores (spinner nos botões,
"Consultando a base de conhecimento e a IA…").

Fluxo de inicialização:

```
Main (JavaFX) → SplashView (Stage transparente) → WebView → index.html + splash.css + splash.js → GSAP
      │                                                         ▲            │
      │  (em paralelo) AppContext.initialize(): conecta ao      │ setStatus  │ javaBridge.introDone()
      │  Atlas, cria índices, cadastra a base inicial ──────────┘            │ javaBridge.finish()
      ▼                                                                      ▼
  Tela de Login  ◄──────────────────────────────── animação de saída concluída
```

## 4. Estrutura de pastas

```
src/main/java/br/com/projeto/
├── Main.java / Launcher.java   inicialização (splash → login); Launcher é a main do instalador
├── config/       AppConfig (variáveis de ambiente), MongoConnection, AppContext (injeção de dependências)
├── controller/   Login, Register, ForgotPassword, Chat, Sidebar, Theme, Knowledge, Settings
├── exception/    exceções com mensagens para o usuário (Validation, Authentication, Database, AiService...)
├── model/        User, Conversation, Message, KnowledgeDocument, PasswordResetToken
├── rag/          TextNormalizer, Tokenizer, StopWords, KeywordExtractor, DocumentScorer, RetrievalResult
├── repository/   interfaces + mongo/ (implementações MongoDB e criação de índices)
├── service/      Authentication, User, Conversation, Chat, RAG, Groq, PasswordRecovery, KnowledgeBase, Theme, Session
├── util/         Validator, PasswordHasher, TokenGenerator, FxAsync, ErrorMessages, DateFormat
└── view/         telas construídas em código + components/ (ícones, balões, Markdown, diálogos)

src/main/resources/
├── css/          base.css, light.css, dark.css
├── images/       ícones da aplicação
├── rag/          stopwords-pt.txt, seed-documents.json (base de conhecimento inicial)
└── splash/       index.html, splash.css, splash.js, gsap.min.js

src/test/java/    testes JUnit + repositórios em memória (support/)
packaging/        icon.ico / icon.png para o instalador
scripts/          build-installer.bat (Windows), build-installer.sh, preview-ui.sh
docs/             TESTES.md, telas/, evidencias/
```

## 5. Pré-requisitos

- JDK 21 (ex.: Eclipse Temurin 21) — inclui o `jpackage`
- Maven 3.9+ (ou o Maven embutido no Eclipse)
- Conta no MongoDB Atlas (cluster gratuito M0 serve)
- Conta na Groq (https://console.groq.com) para gerar a API Key
- Windows 10/11 para gerar o instalador `.exe` + **WiX Toolset 3.x** no `PATH`

## 6. Configuração

Nenhuma credencial fica no código. A aplicação lê, nesta ordem:
**variável de ambiente → propriedade da JVM (`-D`) → arquivo `%USERPROFILE%\.nexuschat\config.properties`**.

| Variável | Obrigatória | Descrição |
|---|---|---|
| `MONGODB_URI` | sim | string de conexão do Atlas (`mongodb+srv://...`) |
| `GROQ_API_KEY` | sim (para conversar) | chave da Groq (`gsk_...`) |
| `MONGODB_DATABASE` | não | nome do banco (padrão `nexuschat`) |
| `GROQ_MODEL` | não | modelo (padrão `llama-3.3-70b-versatile`) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM` | não | envio do código de recuperação por e-mail |

### MongoDB Atlas

1. Crie um cluster (M0).
2. *Database Access*: crie um usuário de banco com senha.
3. *Network Access*: adicione o seu IP (ou `0.0.0.0/0` apenas para testes).
4. *Connect → Drivers → Java*: copie a URI `mongodb+srv://usuario:senha@cluster.xxxxx.mongodb.net/`.

### Groq

Em https://console.groq.com/keys clique em *Create API Key* e copie a chave.

### Definindo no Windows

PowerShell (permanente para o usuário — feche e abra o Eclipse/terminal depois):

```powershell
setx MONGODB_URI "mongodb+srv://usuario:senha@cluster.xxxxx.mongodb.net/"
setx GROQ_API_KEY "gsk_sua_chave"
```

Ou em *Configurações do Windows → Sistema → Sobre → Configurações avançadas → Variáveis de Ambiente*.

**Alternativa (útil na versão instalada):** copie `config.properties.example` para
`%USERPROFILE%\.nexuschat\config.properties` e preencha. Esse arquivo fica fora do projeto e está no `.gitignore`.

**No Eclipse:** *Run → Run Configurations → (sua configuração) → Environment → Add*.

## 7. Banco de dados

Banco `nexuschat` (configurável). Os índices são criados automaticamente na primeira execução.

| Coleção | Campos principais | Índices |
|---|---|---|
| `users` | `_id`, `name`, `email` (minúsculo), `passwordHash` (BCrypt), `createdAt`, `updatedAt` | `email` **único** |
| `conversations` | `_id`, `userId` (dono), `title`, `createdAt`, `updatedAt` | `userId + updatedAt` |
| `messages` | `_id`, `conversationId`, `userId`, `role` (`USER`/`ASSISTANT`), `content`, `sources`, `createdAt` | `conversationId + userId + createdAt` |
| `documents` | `_id`, `ownerId` (`null` = público), `title`, `content`, `keywords`, `createdAt`, `updatedAt` | `keywords`, `ownerId` |
| `password_reset_tokens` | `_id`, `userId`, `tokenHash` (SHA-256), `createdAt`, `expiresAt`, `used`, `attempts` | `userId`, **TTL** em `expiresAt` |

Na primeira execução, se `documents` não tiver documentos públicos, são cadastrados os 12 documentos de
`src/main/resources/rag/seed-documents.json` (Java, JavaFX, MongoDB, MVC, BCrypt, RAG, Groq e políticas
internas fictícias da empresa: home office, suporte técnico e reembolso).

## 8. RAG

Implementação em `rag/` e `service/RAGService.java`:

```
Pergunta: "Qual é a política de home office da empresa?"
  1. Normalização    → "qual e a politica de home office da empresa"   (minúsculas, sem acentos e pontuação)
  2. Tokenização     → [qual, e, a, politica, de, home, office, da, empresa]
  3. Stop words      → remove [qual, e, a, de, da]
  4. Palavras-chave  → [politica, home, office, empresa]   (+ redução simples de plural: "padrões" → "padrao")
  5. Busca MongoDB   → documents com keywords ∈ palavras-chave (públicos + do próprio usuário)
  6. Pontuação       → por termo: 3 pts se é keyword, 2 se está no título, 1 se está no conteúdo
  7. Contexto        → os 3 melhores documentos entram na mensagem "system" enviada à Groq
```

Ao cadastrar um documento, as palavras-chave informadas são normalizadas e complementadas
automaticamente com as palavras do título e as 40 mais frequentes do conteúdo (sem stop words).
A resposta salva guarda em `sources` os títulos usados, exibidos como "Fontes" no chat.

### Por que remover stop words?

Stop words (artigos, preposições, conjunções, pronomes e verbos de pedido como "a", "de", "para", "que",
"explique") aparecem em praticamente **todas** as frases, então não ajudam a descobrir **sobre o que** é a
pergunta. Se fossem mantidas, quase todo documento "combinaria" com qualquer pergunta (todos têm "de" e
"para"), a pontuação ficaria distorcida e a busca traria contexto irrelevante para a IA. Removendo-as,
sobram os termos que carregam significado ("política", "home office", "suporte") e a busca fica mais
precisa, rápida e com menos ruído no prompt. A lista está em `src/main/resources/rag/stopwords-pt.txt`.

## 9. Execução (desenvolvimento)

Com as variáveis configuradas:

```bash
mvn clean javafx:run      # executa a aplicação
mvn test                  # 37 testes automatizados (não precisam de banco nem internet)
```

**No Eclipse:** *File → Import → Maven → Existing Maven Projects*, selecione a pasta do projeto e execute
`br.com.projeto.Launcher` como *Java Application* (ou *Run As → Maven build...* com o goal `javafx:run`).

## 10. Instalação (versão distribuível)

### Gerar o instalador (Windows)

1. Instale o **WiX Toolset 3.14** e adicione a pasta `bin` dele ao `PATH`.
2. Na raiz do projeto:

```bat
scripts\build-installer.bat          :: gera target\installer\NexusChat-1.0.0.exe
scripts\build-installer.bat msi      :: alternativa .msi
scripts\build-installer.bat app-image :: pasta executável, sem precisar do WiX
```

O script executa `mvn clean package` (gera `target/dist/nexus-chat.jar` + `target/dist/libs/`) e depois
o `jpackage`, que embute um runtime Java reduzido (jlink) — **o computador de destino não precisa ter Java**.
O instalador tem nome e versão, ícone personalizado, permite escolher a pasta, cria atalho na área de
trabalho e entrada no menu Iniciar (grupo "Nexus Chat").

### Instalar e usar em outro computador

1. Execute `NexusChat-1.0.0.exe` e siga o assistente.
2. Configure `MONGODB_URI` e `GROQ_API_KEY` (variáveis de ambiente **ou** `%USERPROFILE%\.nexuschat\config.properties`).
3. Libere o IP desse computador em *Network Access* no Atlas.
4. Abra **Nexus Chat** pelo menu Iniciar.

Se a conexão falhar, a aplicação mostra uma tela explicativa com o botão **Tentar novamente**
(que relê o `config.properties`).

## 11. Segurança

- **Senhas:** hash **BCrypt** com salt aleatório e custo 12 (`util/PasswordHasher`); nunca são gravadas
  nem exibidas. A senha exige mínimo de 8 caracteres com letras e números.
- **Recuperação de senha:** a senha antiga não é recuperável. É gerado um código de 6 dígitos com
  `SecureRandom`, salvo apenas como **hash SHA-256**, válido por **15 minutos**, de **uso único**, invalidado
  após **5 tentativas** e quando um novo código é pedido; o MongoDB apaga códigos expirados (índice TTL).
  Com SMTP configurado, o código vai por e-mail; sem SMTP a aplicação está em *modo demonstração* e
  mostra o código na tela (ver Limitações).
- **API Keys e URI do banco:** lidas de variáveis de ambiente / arquivo fora do projeto. `.gitignore`
  bloqueia `.env` e `config.properties`; só o `config.properties.example` (sem valores reais) é versionado.
- **Conexão segura:** `mongodb+srv://` usa TLS por padrão; o cliente usa a Stable API v1 do Atlas.
- **Sessão:** `SessionManager` guarda o usuário autenticado em memória; os Services obtêm o dono dos
  dados **sempre da sessão**, nunca de valores vindos da tela. O logout limpa a sessão e volta ao login;
  operações sem sessão lançam `AuthenticationException`.
- **Isolamento de dados:** toda consulta/alteração/exclusão de conversa e mensagem filtra por
  `_id` **e** `userId` no próprio MongoDB (`Filters.and(eq("_id"), eq("userId"))`); se o registro não
  pertencer ao usuário, o resultado é "não encontrado". Documentos da base só podem ser excluídos pelo dono.
  Comprovado em `ConversationIsolationTest` e `RagServiceTest`.
- **Validação de entrada:** campos obrigatórios, formato de e-mail, tamanho de senha, título (≤ 80) e
  mensagem (≤ 4000 caracteres), tanto na tela quanto nos Services.
- **Erros:** exceções são convertidas em mensagens compreensíveis (`util/ErrorMessages`); detalhes
  técnicos vão só para o log.

## 12. Limitações conhecidas

- O RAG é por **palavras-chave** (sem embeddings/sinônimos): perguntas com palavras diferentes das do
  documento podem não encontrá-lo. A redução de plural é simples e pode errar em alguns casos.
- Sem SMTP configurado, a recuperação de senha funciona em **modo demonstração** (o código aparece na tela).
- As mensagens de login diferenciam "usuário inexistente" e "senha incorreta" (pedido do enunciado), o que
  permite descobrir se um e-mail está cadastrado. Não há bloqueio por excesso de tentativas de login.
- A sessão é apenas em memória: fechar a aplicação exige novo login.
- A resposta da IA chega completa (sem *streaming*); são enviadas as 10 últimas mensagens como memória.
- A preferência de tema é salva por computador, não por usuário.
- O instalador `.exe` exige gerar no Windows com WiX 3.x; as variáveis de ambiente precisam ser
  configuradas em cada máquina.
- A splash usa o `WebView`; se ele não estiver disponível, a aplicação segue direto para o login.

## 13. Testes

`mvn test` executa 37 testes (autenticação, isolamento, conversas, IA com cliente falso, RAG, stop words,
recuperação de senha e segurança). O roteiro dos testes manuais e das evidências está em
[`docs/TESTES.md`](docs/TESTES.md).

Para gerar capturas das telas sem banco (Linux com Xvfb): `scripts/preview-ui.sh` → `target/preview/`.
