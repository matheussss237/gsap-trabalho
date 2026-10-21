# Roteiro de testes

## 1. Testes automatizados (`mvn test` ou configuração "Testes" no IntelliJ)

22 testes JUnit 5 que sobem o Spring de verdade com um **MongoDB falso em memória**
(mongo-java-server) e uma IA falsa. Não precisam do Atlas nem de internet:

| Classe | O que comprova |
|---|---|
| `UsuarioServiceTest` | cadastro válido (senha salva como hash BCrypt), cadastro duplicado, campos inválidos, senha incorreta, usuário inexistente, login/logout |
| `ConversaServiceTest` | criar, renomear, excluir e recarregar; **usuario1 ≠ conversas de usuario2** (o usuario2 não abre, não renomeia e não exclui a conversa do usuario1) |
| `ChatERagTest` | envio de pergunta, histórico salvo, documento do RAG e nome do usuário enviados à IA, memória da conversa, erro da API sem salvar nada; normalização, **remoção de stop words**, palavra-chave existente e inexistente, cadastro de documento |
| `RecuperacaoSenhaServiceTest` | fluxo completo, código vencido, limite de tentativas, código de uso único, e-mail inexistente |

## 2. Testes manuais (com MongoDB Atlas e Groq reais)

Marque cada item e salve a captura em `docs/evidencias/`.

### Autenticação
- [ ] Cadastro válido → mensagem "Conta criada com sucesso" na tela de login
- [ ] Cadastro com e-mail já existente → "Já existe uma conta cadastrada com este e-mail."
- [ ] Senha incorreta → "Senha incorreta…"
- [ ] Usuário inexistente → "Não existe usuário cadastrado com este e-mail."
- [ ] Logout → volta para o login com "Você saiu da sua conta."
- [ ] Recuperação de senha → solicitar código, informar código + nova senha, entrar com a nova senha
- [ ] Código errado 5 vezes → código invalidado

### Conversas
- [ ] Criar ("Nova conversa")
- [ ] Carregar (clicar na conversa na sidebar)
- [ ] Renomear (ícone de lápis, menu de contexto ou cabeçalho)
- [ ] Excluir (confirmação antes de excluir)
- [ ] Fechar o app, abrir novamente e verificar que o histórico continua lá

### Isolamento
- [ ] Criar `usuario1` com conversas "Java", "JavaFX", "MongoDB"
- [ ] Criar `usuario2` com conversas "Angular", "Spring", "JavaScript"
- [ ] Logar com cada um e comprovar que um não vê as conversas do outro
- [ ] No Atlas, mostrar o campo `userId` das conversas de cada usuário

### IA
- [ ] Enviar pergunta e receber resposta
- [ ] Remover/alterar `GROQ_API_KEY` → mensagem amigável ("A chave da Groq não foi configurada…" / "…é inválida…")
- [ ] Desligar a internet → "Sem conexão com a internet ou com a API da Groq…"

### RAG
- [ ] Base de conhecimento → cadastrar documento (ex.: "Política de férias")
- [ ] Testar recuperação com palavra existente ("Como funcionam as férias?") → documento listado com pontuação
- [ ] Testar com palavra inexistente ("receita de bolo") → "Nenhum documento encontrado"
- [ ] Verificar a seção "Stop words removidas" (ex.: "qual", "e", "o", "do")
- [ ] No chat, perguntar "Qual é a política de home office da empresa?" → resposta cita 3 dias/semana e mostra "Fontes"

### Interface
- [ ] Tema claro e tema escuro (botão sol/lua) — a escolha persiste ao reabrir
- [ ] Navegação login ↔ cadastro ↔ recuperação
- [ ] Redimensionar: abaixo de ~820 px a sidebar recolhe (botão ☰ reabre) e o painel da marca do login some

### Instalador
- [ ] Gerar com `scripts\build-installer.bat`
- [ ] Instalar, abrir pelo menu Iniciar/atalho
- [ ] Login, conversa, IA, persistência, logout e outro usuário na versão instalada
- [ ] Se possível, repetir em outro computador
