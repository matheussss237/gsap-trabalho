package br.com.projeto.config;

import br.com.projeto.exception.ConfigurationException;
import br.com.projeto.exception.DatabaseException;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

/**
 * Mantém uma única instância de MongoClient (thread-safe) para toda a aplicação.
 * A URI vem de MONGODB_URI; com "mongodb+srv://" (Atlas) a conexão usa TLS automaticamente.
 */
public final class MongoConnection {

    private static MongoConnection instance;

    private final MongoClient client;
    private final MongoDatabase database;

    private MongoConnection() {
        String uri = AppConfig.get(AppConfig.MONGODB_URI).orElseThrow(() -> new ConfigurationException(
                "A variável MONGODB_URI não foi configurada. Consulte o README para configurar o MongoDB Atlas."));

        ConnectionString connectionString;
        try {
            connectionString = new ConnectionString(uri);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("O valor de MONGODB_URI é inválido. Verifique a string de conexão.", e);
        }

        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToClusterSettings(c -> c.serverSelectionTimeout(8, TimeUnit.SECONDS))
                .applyToSocketSettings(s -> s.connectTimeout(8, TimeUnit.SECONDS));
        if (uri.startsWith("mongodb+srv://")) {
            builder.serverApi(ServerApi.builder().version(ServerApiVersion.V1).build());
        }

        this.client = MongoClients.create(builder.build());
        String dbName = connectionString.getDatabase() != null
                ? connectionString.getDatabase()
                : AppConfig.getOrDefault(AppConfig.MONGODB_DATABASE, AppConfig.DEFAULT_DATABASE);
        this.database = client.getDatabase(dbName);
    }

    public static synchronized MongoConnection getInstance() {
        if (instance == null) {
            instance = new MongoConnection();
        }
        return instance;
    }

    public MongoDatabase database() {
        return database;
    }

    /** Verifica se o cluster responde. Lança DatabaseException com mensagem amigável. */
    public void ping() {
        try {
            database.runCommand(new Document("ping", 1));
        } catch (MongoException e) {
            throw new DatabaseException(
                    "Não foi possível conectar ao MongoDB Atlas. Verifique sua internet, a MONGODB_URI "
                            + "e se o seu IP está liberado em Network Access no Atlas.", e);
        }
    }

    public static synchronized void close() {
        if (instance != null) {
            instance.client.close();
            instance = null;
        }
    }
}
