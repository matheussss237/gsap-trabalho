package br.com.projeto.service;

import br.com.projeto.config.AppConfig;
import br.com.projeto.exception.AiServiceException;
import br.com.projeto.exception.ConfigurationException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Cliente REST da API da Groq (compatível com o formato OpenAI /chat/completions).
 * A chave é lida de GROQ_API_KEY em tempo de execução — nunca está no código-fonte.
 */
public class GroqService implements AiClient {

    private static final URI ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");

    private final HttpClient httpClient;
    private final String model;

    public GroqService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.model = AppConfig.getOrDefault(AppConfig.GROQ_MODEL, AppConfig.DEFAULT_GROQ_MODEL);
    }

    @Override
    public boolean isConfigured() {
        return apiKey().isPresent();
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public String complete(List<AiMessage> messages) {
        String key = apiKey().orElseThrow(() -> new ConfigurationException(
                "A chave da Groq não foi configurada. Defina a variável de ambiente GROQ_API_KEY e reinicie o aplicativo."));

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(messages).toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpConnectTimeoutException | ConnectException | UnknownHostException e) {
            throw new AiServiceException("Sem conexão com a internet ou com a API da Groq. Verifique sua rede.", e);
        } catch (HttpTimeoutException e) {
            throw new AiServiceException("A IA demorou demais para responder. Tente novamente.", e);
        } catch (IOException e) {
            throw new AiServiceException("Não foi possível se comunicar com a API da Groq. Verifique sua internet.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("O envio da mensagem foi interrompido.", e);
        }
        return parse(response);
    }

    private JsonObject buildBody(List<AiMessage> messages) {
        JsonArray array = new JsonArray();
        for (AiMessage message : messages) {
            JsonObject item = new JsonObject();
            item.addProperty("role", message.role().apiName());
            item.addProperty("content", message.content());
            array.add(item);
        }
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", array);
        body.addProperty("temperature", 0.5);
        body.addProperty("max_tokens", 1500);
        return body;
    }

    private String parse(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new AiServiceException("A chave da Groq (GROQ_API_KEY) é inválida ou foi revogada.");
        }
        if (status == 404) {
            throw new AiServiceException("O modelo \"" + model + "\" não está disponível na Groq. Ajuste GROQ_MODEL.");
        }
        if (status == 429) {
            throw new AiServiceException("Limite de uso da Groq atingido. Aguarde alguns instantes e tente novamente.");
        }
        if (status >= 500) {
            throw new AiServiceException("A API da Groq está indisponível no momento (erro " + status + "). Tente mais tarde.");
        }
        if (status != 200) {
            throw new AiServiceException("A API da Groq recusou a requisição (erro " + status + ").",
                    new IllegalStateException(response.body()));
        }
        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement content = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content");
            String text = content == null || content.isJsonNull() ? "" : content.getAsString().trim();
            if (text.isEmpty()) {
                throw new AiServiceException("A IA retornou uma resposta vazia. Tente reformular a pergunta.");
            }
            return text;
        } catch (JsonParseException | IllegalStateException | NullPointerException | IndexOutOfBoundsException e) {
            throw new AiServiceException("A resposta da Groq veio em um formato inesperado.", e);
        }
    }

    private Optional<String> apiKey() {
        return AppConfig.get(AppConfig.GROQ_API_KEY);
    }
}
