# Lumi

**Lumi** é uma aplicação desktop de chat com Inteligência Artificial feita com **Java 21, JavaFX 21,
MongoDB Atlas, RAG por palavras-chave e API da Groq**. A animação de entrada é feita com **GSAP**
dentro de um `WebView`.

<p align="center">
  <img src="docs/telas/01-splash.png" width="560" alt="Animação de entrada (GSAP)">
</p>

| Login (claro) | Chat (escuro) |
|---|---|
| ![Login](docs/telas/03-login-claro.png) | ![Chat](docs/telas/07-chat-escuro.png) |

| Chat (claro) | Painel "Testar recuperação (RAG)" |
|---|---|
| ![Chat claro](docs/telas/08-chat-claro.png) | ![RAG](docs/telas/11-teste-rag.png) |

> As imagens de `docs/telas/` foram geradas com dados de demonstração. As evidências com o
> MongoDB Atlas e a Groq reais ficam em `docs/evidencias/` (veja `docs/TESTES.md`).

---

## 1. Sobre o projeto

A Lumi é uma assistente de IA para os colaboradores de uma empresa. Cada pessoa tem a sua conta e as
suas próprias conversas. As perguntas vão para a Groq junto com trechos relevantes de uma **base de
conhecimento** guardada no MongoDB (RAG), e todo o histórico fica salvo no MongoDB Atlas.

Funcionalidades:

- animação de entrada com **GSAP** (`gsap.timeline`, `from`, `to`, `fromTo`, `stagger`) dentro de um `WebView`;
- cadastro, login, **sessão** e logout;
- recuperação de senha com **código temporário** (15 min, uso único, 5 tentativas);
- conversas **isoladas por usuário**: criar, listar, buscar, renomear e excluir (com confirmação);
- envio de mensagens para a **Groq**, com memória das últimas mensagens da conversa;
- **IA personalizada**: a Lumi recebe o nome de quem está logado e conversa chamando a pessoa pelo nome;
- **RAG** por palavras-chave com remoção de **stop words**, e base de conhecimento com cadastro de documentos;
- tema **claro e escuro** com JavaFX CSS (a escolha fica salva);
- respostas em Markdown (títulos, listas, negrito e blocos de código com botão "Copiar");
- erros mostrados com mensagens compreensíveis;
- instalador Windows gerado com **jpackage**.

### Diferencial

1. **RAG transparente**: cada resposta mostra as **fontes**, ou seja, os documentos da base usados como
   contexto. A janela *Base de conhecimento → Testar recuperação* mostra cada etapa: palavras-chave
   extraídas, stop words removidas e a pontuação de cada documento encontrado.
2. **Splash ligada ao carregamento real**: o Java manda para a animação o status da conexão com o
   MongoDB (`setStatus`), e o GSAP avisa o Java quando a entrada terminou (`javaBridge.introDone`) e
   quando a saída acabou (`javaBridge.finish`). A tela de login só abre quando a animação terminou
   **e** o banco respondeu.
3. **Base com documentos da empresa** (home office, suporte técnico, reembolso). São informações que o
   modelo não conhece sozinho, então o efeito do RAG fica fácil de mostrar na apresentação.

## 2. Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | linguagem |
| JavaFX 21 (`javafx-controls`, `javafx-web`) | interface feita só com código (sem Scene Builder) e o `WebView` da splash |
| JavaFX CSS | estilo e temas (`base.css`, `light.css`, `dark.css`) |
| GSAP 3.13 (embutido em `resources/splash`) | animação de entrada |
| Maven | build e dependências (`pom.xml`) |
| MongoDB Atlas + `mongodb-driver-sync` 5 | banco de dados |
| Groq API (`/openai/v1/chat/completions`) via `java.net.http.HttpClient` | IA |
| Gson | JSON da API da Groq |
| jBCrypt | hash das senhas |
| Angus Mail (Jakarta Mail) | envio opcional do código de recuperação por e-mail |
| JUnit 5 | testes |
| IntelliJ IDEA | ambiente de desenvolvimento |
| jpackage + WiX Toolset | instalador Windows |
| Git / GitHub | versionamento |

## 3. Arquitetura

```
View  →  Controller  →  Service  →  Repository  →  MongoDB
```

- **View** só monta a interface (`AuthView`, `ChatView`, `KnowledgeView`…).
- **Controller** recebe os cliques da View e chama os Services. Nunca acessa o banco.
- **Service** tem as regras de negócio e as validações (`AuthenticationService`, `ChatService`, `RAGService`…).
- **Repository** é a única camada que fala com o MongoDB (uma classe por coleção).

Caminho de uma pergunta:

```
ChatView → ChatController → ChatService → RAGService  (busca na coleção documents)
                                        → GroqService → Groq API
                                        → MessageRepository (salva pergunta e resposta) → MongoDB
         ← a resposta aparece no JavaFX
```

As operações demoradas (MongoDB e Groq) rodam fora da thread do JavaFX, com `javafx.concurrent.Task`
(`Ui.async`), então a tela não trava. Enquanto isso aparecem indicadores: spinner nos botões e
"A Lumi está consultando a base de conhecimento…".

Inicialização:

```
Main → SplashView (janela transparente) → WebView → index.html + splash.css + splash.js → GSAP
  │                                                     ▲ setStatus        │ javaBridge.introDone()
  │  ao mesmo tempo: AppConfig.connect() conecta ao      │                  │ javaBridge.finish()
  │  Atlas, cria os índices e cadastra a base inicial ───┘                  ▼
  └──────────────────────────────────────────────────► Tela de login (depois da animação de saída)
```

## 4. Estrutura de pastas

São **31 classes** em 8 pastas, que seguem a estrutura pedida no enunciado:

```
src/main/java/br/com/projeto/
├── Main.java        início do app (splash → login), monta os Services e faz a navegação entre telas
├── config/          AppConfig: variáveis de ambiente + conexão com o MongoDB Atlas e índices
├── model/           User, Conversation, Message, KnowledgeDocument, PasswordResetToken
├── repository/      UserRepository, ConversationRepository, MessageRepository,
│                    DocumentRepository, PasswordResetRepository (uma por coleção)
├── rag/             TextProcessor: normalização, tokenização, stop words e palavras-chave
├── service/         AuthenticationService, PasswordRecoveryService, ConversationService,
│                    ChatService, RAGService, GroqService
├── controller/      AuthController (login, cadastro, recuperação), ChatController, ThemeController
├── view/            SplashView, AuthView, ChatView, MessageView, KnowledgeView, Components
└── util/            AppException, Security (validação, BCrypt, códigos), Ui (tarefas, erros, diálogos)

src/main/resources/
├── css/             base.css, light.css, dark.css
├── images/          ícones da aplicação
├── rag/             stopwords-pt.txt, seed-documents.json (base de conhecimento inicial)
└── splash/          index.html, splash.css, splash.js, gsap.min.js

src/test/java/       testes JUnit (com banco em memória) e a pré-visualização das telas
.run/                configurações de execução do IntelliJ ("Lumi" e "Testes")
packaging/           icon.ico / icon.png do instalador
scripts/             build-installer.bat (Windows), build-installer.sh, preview-ui.sh
docs/                TESTES.md, telas/, evidencias/
```

## 5. Pré-requisitos

- **IntelliJ IDEA** (a versão gratuita serve)
- **JDK 21**: o IntelliJ consegue baixar (Eclipse Temurin 21). O JDK já traz o `jpackage`.
- Maven: já vem dentro do IntelliJ; para o instalador, instale o Maven ou use o do IntelliJ
- Conta no **MongoDB Atlas** (o cluster gratuito M0 serve)
- Conta na **Groq** (https://console.groq.com) para gerar a API Key
- Windows 10/11 + **WiX Toolset 3.x** para gerar o instalador `.exe`

## 6. Configuração

Nenhuma senha ou chave fica no código. A Lumi procura cada configuração nesta ordem:
**variável de ambiente → propriedade da JVM (`-D`) → arquivo `%USERPROFILE%\.lumi\config.properties`**.

| Variável | Obrigatória | Para que serve |
|---|---|---|
| `MONGODB_URI` | sim | endereço de conexão do Atlas (`mongodb+srv://...`) |
| `GROQ_API_KEY` | sim (para conversar) | chave da Groq (`gsk_...`) |
| `MONGODB_DATABASE` | não | nome do banco (padrão `lumi`) |
| `GROQ_MODEL` | não | modelo (padrão `llama-3.3-70b-versatile`) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM` | não | enviar o código de recuperação por e-mail |

### MongoDB Atlas

1. Crie um cluster (M0).
2. Em *Database Access*, crie um usuário de banco com senha.
3. Em *Network Access*, adicione o seu IP (ou `0.0.0.0/0` só para testes).
4. Em *Connect → Drivers → Java*, copie a URI `mongodb+srv://usuario:senha@cluster.xxxxx.mongodb.net/`.

### Groq

Em https://console.groq.com/keys, clique em *Create API Key* e copie a chave.

### Onde colocar as variáveis

Escolha **uma** das opções:

- **IntelliJ (mais simples para desenvolver):** *Run → Edit Configurations → Lumi → Environment variables*:
  `MONGODB_URI=...;GROQ_API_KEY=...`
- **Windows (vale para tudo, inclusive o app instalado).** No PowerShell, rode os comandos abaixo e
  depois feche e abra o IntelliJ:
  ```powershell
  setx MONGODB_URI "mongodb+srv://usuario:senha@cluster.xxxxx.mongodb.net/"
  setx GROQ_API_KEY "gsk_sua_chave"
  ```
- **Arquivo:** copie `config.properties.example` para `%USERPROFILE%\.lumi\config.properties` e preencha.
  Esse arquivo fica fora do projeto e está no `.gitignore`.

## 7. Banco de dados

Banco `lumi` (dá para mudar). Os índices são criados sozinhos na primeira execução.

| Coleção | Campos principais | Índices |
|---|---|---|
| `users` | `_id`, `name`, `email` (minúsculo), `passwordHash` (BCrypt), `createdAt`, `updatedAt` | `email` **único** |
| `conversations` | `_id`, `userId` (dono), `title`, `createdAt`, `updatedAt` | `userId + updatedAt` |
| `messages` | `_id`, `conversationId`, `userId`, `role` (`USER`/`ASSISTANT`), `content`, `sources`, `createdAt` | `conversationId + userId + createdAt` |
| `documents` | `_id`, `ownerId` (`null` = público), `title`, `content`, `keywords`, `createdAt`, `updatedAt` | `keywords` |
| `password_reset_tokens` | `_id`, `userId`, `tokenHash` (SHA-256), `createdAt`, `expiresAt`, `used`, `attempts` | `userId`, **TTL** em `expiresAt` |

Na primeira execução, se `documents` estiver vazia, a Lumi cadastra os 12 documentos de
`src/main/resources/rag/seed-documents.json`: Java, JavaFX, MongoDB, MVC, BCrypt, RAG, Groq e políticas
fictícias da empresa (home office, suporte técnico e reembolso).

## 8. RAG

O código está em `rag/TextProcessor.java` e `service/RAGService.java`:

```
Pergunta: "Qual é a política de home office da empresa?"
  1. Normalização    → "qual e a politica de home office da empresa"   (minúsculas, sem acento e pontuação)
  2. Tokenização     → [qual, e, a, politica, de, home, office, da, empresa]
  3. Stop words      → remove [qual, e, a, de, da]
  4. Palavras-chave  → [politica, home, office, empresa]   (+ plural simples: "padrões" → "padrao")
  5. Busca no MongoDB → documents com keywords ∈ palavras-chave (públicos + do próprio usuário)
  6. Pontuação       → por palavra: 3 pontos se é keyword, 2 se está no título, 1 se está no conteúdo
  7. Contexto        → os 3 melhores documentos vão na mensagem "system" enviada à Groq
```

Ao cadastrar um documento, a Lumi normaliza as palavras-chave digitadas e completa com as palavras do
título e as 40 mais frequentes do conteúdo (sem stop words). A resposta salva guarda em `sources` os
títulos usados, que aparecem como "Fontes" no chat.

### Por que remover stop words?

Stop words são artigos, preposições, conjunções, pronomes e verbos de pedido, como "a", "de", "para",
"que" e "explique". Elas aparecem em quase **todas** as frases, então não ajudam a descobrir **sobre o
que** é a pergunta. Se ficassem, quase todo documento "combinaria" com qualquer pergunta (todos têm "de"
e "para"), a pontuação ficaria errada e a IA receberia contexto que não tem nada a ver. Tirando essas
palavras, sobram as que têm significado ("política", "home office", "suporte"), e a busca fica mais
precisa e rápida. A lista está em `src/main/resources/rag/stopwords-pt.txt`.

## 9. Execução no IntelliJ

1. *File → Open* e escolha a pasta do projeto (a que tem o `pom.xml`). O IntelliJ reconhece o Maven e
   baixa as dependências.
2. *File → Project Structure → Project → SDK*: escolha o **JDK 21**. Se não tiver, use *Download JDK →
   Temurin 21*.
3. Configure as variáveis (seção 6).
4. No topo da janela, escolha a configuração **Lumi** e clique em ▶. Ela roda a classe
   `br.com.projeto.Main$Launcher`.
5. Para rodar os testes, escolha a configuração **Testes** e clique em ▶ (ou use `mvn test`).

Pela linha de comando: `mvn javafx:run` (executa) e `mvn test` (23 testes, sem precisar de banco nem de internet).

## 10. Instalação (versão distribuível)

### Gerar o instalador (Windows)

1. Instale o **WiX Toolset 3.14** e coloque a pasta `bin` dele no `PATH`.
2. Na pasta do projeto, pelo terminal do IntelliJ:

```bat
scripts\build-installer.bat           :: gera target\installer\Lumi-1.0.0.exe
scripts\build-installer.bat msi       :: opção .msi
scripts\build-installer.bat app-image :: pasta com o executável, sem precisar do WiX
```

O script roda `mvn clean package`, que gera `target/dist/lumi.jar` e `target/dist/libs/`, e depois o
`jpackage`. O `jpackage` coloca junto um Java reduzido, então **o computador de destino não precisa ter
Java instalado**. O instalador tem nome, versão e ícone, deixa escolher a pasta e cria atalho na área
de trabalho e no menu Iniciar.

### Instalar em outro computador

1. Execute `Lumi-1.0.0.exe` e siga o assistente.
2. Configure `MONGODB_URI` e `GROQ_API_KEY` (com `setx` ou com `%USERPROFILE%\.lumi\config.properties`).
3. Libere o IP desse computador em *Network Access* no Atlas.
4. Abra a **Lumi** pelo menu Iniciar.

Se a conexão falhar, aparece uma tela explicando o problema, com o botão **Tentar novamente** (que lê
de novo o `config.properties`).

## 11. Segurança

- **Senhas:** hash **BCrypt** com salt aleatório e custo 12 (`util/Security`). A senha nunca é gravada
  nem mostrada. É exigido no mínimo 8 caracteres, com letras e números.
- **Recuperação de senha:** a senha antiga não pode ser recuperada. É gerado um código de 6 dígitos com
  `SecureRandom`, e no banco fica só o **hash SHA-256** dele. O código vale **15 minutos**, só pode ser
  usado **uma vez** e é invalidado depois de **5 tentativas erradas** ou quando um novo código é pedido.
  O próprio MongoDB apaga os códigos vencidos (índice TTL). Com SMTP configurado, o código vai por
  e-mail; sem SMTP, a Lumi fica em *modo demonstração* e mostra o código na tela (ver Limitações).
- **API Keys e endereço do banco:** vêm de variáveis de ambiente ou de um arquivo fora do projeto. O
  `.gitignore` bloqueia `.env` e `config.properties`; só o `config.properties.example`, sem valores
  reais, vai para o GitHub.
- **Conexão segura:** `mongodb+srv://` usa TLS por padrão.
- **Sessão:** o `AuthenticationService` guarda em memória quem está logado. Os Services pegam o dono
  dos dados **sempre da sessão**, nunca de algo que veio da tela. O logout limpa a sessão e volta para
  o login; qualquer operação sem sessão é recusada.
- **Isolamento de dados:** toda consulta, alteração e exclusão de conversa e de mensagem filtra por
  `_id` **e** `userId` dentro do próprio MongoDB. Se o registro não for do usuário, o resultado é
  "não encontrado". Documentos da base só podem ser excluídos por quem os cadastrou. Isso é comprovado
  em `ConversationIsolationTest` e `ChatAndRagTest`.
- **Validação:** campos obrigatórios, formato de e-mail, tamanho da senha, título (até 80 caracteres) e
  mensagem (até 4000 caracteres), tanto na tela quanto nos Services.
- **Erros:** as exceções viram mensagens compreensíveis (`Ui.friendly`); os detalhes técnicos vão só
  para o log.

## 12. Limitações conhecidas

- O RAG é por **palavras-chave**, sem sinônimos: se a pergunta usar palavras diferentes das do
  documento, ele pode não ser encontrado. A redução de plural é simples e pode errar em alguns casos.
- Sem SMTP configurado, a recuperação de senha fica em **modo demonstração** (o código aparece na tela).
- O login diferencia "usuário inexistente" de "senha incorreta" (o enunciado testa os dois casos). Isso
  permite descobrir se um e-mail está cadastrado. Também não há bloqueio depois de muitas tentativas.
- A sessão fica só na memória: ao fechar a Lumi, é preciso fazer login de novo.
- A resposta da IA chega inteira (sem aparecer aos poucos), e só as 10 últimas mensagens vão como memória.
- O tema fica salvo por computador, não por usuário.
- O `.exe` precisa ser gerado no Windows com o WiX 3.x, e as variáveis precisam ser configuradas em
  cada computador.

## 13. Testes

`mvn test` (ou a configuração **Testes** do IntelliJ) roda 23 testes: autenticação, isolamento entre
usuários, conversas, IA (com uma IA falsa), RAG, stop words e recuperação de senha. O roteiro dos
testes manuais e das evidências está em [`docs/TESTES.md`](docs/TESTES.md).

Para gerar capturas das telas sem banco (Linux com Xvfb): `scripts/preview-ui.sh` → `target/preview/`.
