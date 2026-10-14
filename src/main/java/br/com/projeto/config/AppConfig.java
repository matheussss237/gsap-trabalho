package br.com.projeto.config;

import br.com.projeto.util.AppException;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Configuração externa e conexão com o MongoDB Atlas. Nenhuma credencial fica no código.
 *
 * Cada chave (ex.: GROQ_API_KEY) é buscada nesta ordem:
 *   1. variável de ambiente;
 *   2. propriedade da JVM (-DGROQ_API_KEY=...);
 *   3. arquivo %USERPROFILE%/.lumi/config.properties (útil na versão instalada).
 */
public final class AppConfig {

    public static final String APP_NAME = "Lumi";
    public static final String APP_VERSION = "1.0.0";
    public static final String DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile";

    private static final Path CONFIG_FILE = Path.of(System.getProperty("user.home"), ".lumi", "config.properties");
    private static volatile Properties fileProperties = loadFile();
    private static MongoClient client;
    private static MongoDatabase database;

    private AppConfig() {
    }

    // ---------- variáveis de configuração ----------

    public static Optional<String> get(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) value = System.getProperty(key);
        if (value == null || value.isBlank()) value = fileProperties.getProperty(key);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    public static String get(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public static Path configFile() {
        return CONFIG_FILE;
    }

    /** Relê o arquivo de configuração (botão "Tentar novamente"). */
    public static void reload() {
        fileProperties = loadFile();
    }

    private static Properties loadFile() {
        Properties properties = new Properties();
        if (Files.isRegularFile(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                properties.load(in);
            } catch (IOException e) {
                System.err.println("Não foi possível ler " + CONFIG_FILE + ": " + e.getMessage());
            }
        }
        return properties;
    }

    // ---------- MongoDB ----------

    /**
     * Conecta ao Atlas (uma única vez), testa com "ping" e cria os índices.
     * Com "mongodb+srv://" a conexão usa TLS automaticamente.
     */
    public static synchronized MongoDatabase connect() {
        if (database != null) {
            return database;
        }
        String uri = get("MONGODB_URI").orElseThrow(() -> new AppException(
                "A variável MONGODB_URI não foi configurada. Consulte o README para configurar o MongoDB Atlas."));
        ConnectionString connection;
        try {
            connection = new ConnectionString(uri);
        } catch (IllegalArgumentException e) {
            throw new AppException("O valor de MONGODB_URI é inválido. Verifique a string de conexão.", e);
        }
        MongoClientSettings.Builder settings = MongoClientSettings.builder()
                .applyConnectionString(connection)
                .applyToClusterSettings(c -> c.serverSelectionTimeout(8, TimeUnit.SECONDS))
                .applyToSocketSettings(s -> s.connectTimeout(8, TimeUnit.SECONDS));
        if (uri.startsWith("mongodb+srv://")) {
            settings.serverApi(ServerApi.builder().version(ServerApiVersion.V1).build());
        }

        MongoClient newClient = MongoClients.create(settings.build());
        String dbName = connection.getDatabase() != null ? connection.getDatabase() : get("MONGODB_DATABASE", "lumi");
        MongoDatabase db = newClient.getDatabase(dbName);
        try {
            db.runCommand(new Document("ping", 1));
            createIndexes(db);
        } catch (MongoException e) {
            newClient.close();
            throw new AppException("Não foi possível conectar ao MongoDB Atlas. Verifique sua internet, a MONGODB_URI "
                    + "e se o seu IP está liberado em Network Access no Atlas.", e);
        }
        client = newClient;
        database = db;
        return db;
    }

    private static void createIndexes(MongoDatabase db) {
        db.getCollection("users").createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
        db.getCollection("conversations").createIndex(
                Indexes.compoundIndex(Indexes.ascending("userId"), Indexes.descending("updatedAt")));
        db.getCollection("messages").createIndex(Indexes.compoundIndex(
                Indexes.ascending("conversationId"), Indexes.ascending("userId"), Indexes.ascending("createdAt")));
        db.getCollection("documents").createIndex(Indexes.ascending("keywords"));
        db.getCollection("password_reset_tokens").createIndex(Indexes.ascending("userId"));
        // TTL: o próprio MongoDB apaga os códigos expirados
        db.getCollection("password_reset_tokens").createIndex(Indexes.ascending("expiresAt"),
                new IndexOptions().expireAfter(0L, TimeUnit.SECONDS));
    }

    public static synchronized void disconnect() {
        if (client != null) {
            client.close();
            client = null;
            database = null;
        }
    }

    // ---------- auxiliares usados pelos repositories ----------

    /** Executa uma operação no banco convertendo falhas do driver em mensagens amigáveis. */
    public static <T> T db(String action, Supplier<T> operation) {
        try {
            return operation.get();
        } catch (MongoTimeoutException e) {
            throw new AppException("O banco de dados não respondeu ao " + action
                    + ". Verifique sua conexão com a internet e o MongoDB Atlas.", e);
        } catch (MongoException e) {
            throw new AppException("Ocorreu um erro no banco de dados ao " + action + ".", e);
        }
    }

    /** Converte o id textual em ObjectId; ids inválidos simplesmente não existem. */
    public static Optional<ObjectId> objectId(String id) {
        return id != null && ObjectId.isValid(id) ? Optional.of(new ObjectId(id)) : Optional.empty();
    }

    public static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    public static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
