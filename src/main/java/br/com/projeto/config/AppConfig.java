package br.com.projeto.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Configuração externa da aplicação. Nenhuma credencial fica no código-fonte.
 *
 * Ordem de busca de cada chave:
 *   1. variável de ambiente (ex.: GROQ_API_KEY);
 *   2. propriedade de sistema da JVM (-DGROQ_API_KEY=...);
 *   3. arquivo %USERPROFILE%/.nexuschat/config.properties (útil na versão instalada).
 */
public final class AppConfig {

    public static final String APP_NAME = "Nexus Chat";
    public static final String APP_VERSION = "1.0.0";

    public static final String MONGODB_URI = "MONGODB_URI";
    public static final String MONGODB_DATABASE = "MONGODB_DATABASE";
    public static final String GROQ_API_KEY = "GROQ_API_KEY";
    public static final String GROQ_MODEL = "GROQ_MODEL";
    public static final String SMTP_HOST = "SMTP_HOST";
    public static final String SMTP_PORT = "SMTP_PORT";
    public static final String SMTP_USER = "SMTP_USER";
    public static final String SMTP_PASSWORD = "SMTP_PASSWORD";
    public static final String SMTP_FROM = "SMTP_FROM";

    public static final String DEFAULT_DATABASE = "nexuschat";
    public static final String DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile";

    private static final Path CONFIG_FILE =
            Path.of(System.getProperty("user.home"), ".nexuschat", "config.properties");

    private static volatile Properties fileProperties = loadFile();

    private AppConfig() {
    }

    public static Optional<String> get(String key) {
        String value = System.getenv(key);
        if (isBlank(value)) {
            value = System.getProperty(key);
        }
        if (isBlank(value)) {
            value = fileProperties.getProperty(key);
        }
        return isBlank(value) ? Optional.empty() : Optional.of(value.trim());
    }

    public static String getOrDefault(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /** Relê o arquivo de configuração (usado no "Tentar novamente"). */
    public static void reload() {
        fileProperties = loadFile();
    }

    public static Path configFile() {
        return CONFIG_FILE;
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
