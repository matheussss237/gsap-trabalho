package br.com.projeto.config;

import br.com.projeto.repository.ConversationRepository;
import br.com.projeto.repository.DocumentRepository;
import br.com.projeto.repository.MessageRepository;
import br.com.projeto.repository.PasswordResetRepository;
import br.com.projeto.repository.UserRepository;
import br.com.projeto.repository.mongo.MongoConversationRepository;
import br.com.projeto.repository.mongo.MongoDocumentRepository;
import br.com.projeto.repository.mongo.MongoIndexes;
import br.com.projeto.repository.mongo.MongoMessageRepository;
import br.com.projeto.repository.mongo.MongoPasswordResetRepository;
import br.com.projeto.repository.mongo.MongoUserRepository;
import br.com.projeto.service.AiClient;
import br.com.projeto.service.AuthenticationService;
import br.com.projeto.service.ChatService;
import br.com.projeto.service.ConversationService;
import br.com.projeto.service.EmailService;
import br.com.projeto.service.GroqService;
import br.com.projeto.service.KnowledgeBaseService;
import br.com.projeto.service.PasswordRecoveryService;
import br.com.projeto.service.RAGService;
import br.com.projeto.service.SessionManager;
import br.com.projeto.service.SmtpEmailService;
import br.com.projeto.service.UserService;
import com.mongodb.client.MongoDatabase;

import java.time.Clock;
import java.util.Optional;

/**
 * Monta as dependências da aplicação (injeção de dependências manual):
 * Repositories (MongoDB) → Services → disponíveis para os Controllers.
 */
public final class AppContext {

    private final SessionManager session = new SessionManager();
    private final AiClient aiClient;
    private final Optional<EmailService> emailService;

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final ConversationService conversationService;
    private final RAGService ragService;
    private final ChatService chatService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final PasswordRecoveryService passwordRecoveryService;

    /** Permite montar o contexto com outras implementações (ex.: repositórios em memória nos testes). */
    public AppContext(UserRepository userRepository, ConversationRepository conversationRepository,
                      MessageRepository messageRepository, DocumentRepository documentRepository,
                      PasswordResetRepository passwordResetRepository, AiClient aiClient,
                      Optional<EmailService> emailService) {
        this.aiClient = aiClient;
        this.emailService = emailService;
        userService = new UserService(userRepository, session);
        authenticationService = new AuthenticationService(userService, session);
        conversationService = new ConversationService(conversationRepository, messageRepository, session);
        ragService = new RAGService(documentRepository, session);
        chatService = new ChatService(conversationService, messageRepository, ragService, aiClient, session);
        knowledgeBaseService = new KnowledgeBaseService(documentRepository, session);
        passwordRecoveryService = new PasswordRecoveryService(userService, passwordResetRepository,
                emailService, Clock.systemUTC());
    }

    /**
     * Conecta ao MongoDB Atlas, cria índices e cadastra a base de conhecimento inicial.
     * Executado em segundo plano enquanto a animação GSAP é exibida.
     */
    public static AppContext initialize() {
        MongoConnection connection = MongoConnection.getInstance();
        connection.ping();
        MongoIndexes.ensure(connection.database());
        MongoDatabase db = connection.database();
        AppContext context = new AppContext(new MongoUserRepository(db), new MongoConversationRepository(db),
                new MongoMessageRepository(db), new MongoDocumentRepository(db), new MongoPasswordResetRepository(db),
                new GroqService(), SmtpEmailService.fromConfig());
        context.knowledgeBaseService.seedIfEmpty();
        return context;
    }

    public SessionManager session() { return session; }
    public AiClient aiClient() { return aiClient; }
    public boolean emailConfigured() { return emailService.isPresent(); }
    public UserService userService() { return userService; }
    public AuthenticationService authenticationService() { return authenticationService; }
    public ConversationService conversationService() { return conversationService; }
    public RAGService ragService() { return ragService; }
    public ChatService chatService() { return chatService; }
    public KnowledgeBaseService knowledgeBaseService() { return knowledgeBaseService; }
    public PasswordRecoveryService passwordRecoveryService() { return passwordRecoveryService; }
}
