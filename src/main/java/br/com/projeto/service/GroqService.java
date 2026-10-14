package br.com.projeto.service;

import br.com.projeto.config.AppConfig;
import br.com.projeto.model.Message;
import br.com.projeto.util.AppException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;

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

/**
 * Cliente REST da API da Groq (formato compatível com OpenAI /chat/completions).
 * A chave vem de GROQ_API_KEY em tempo de execução — nunca está no código-fonte.
 */
public class GroqService {

    /** Uma mensagem do prompt enviado ao modelo (papel + texto). */
    public record Turn(Message.Role role, String content) {
    }

    private static final URI ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String model = AppConfig.get("GROQ_MODEL", AppConfig.DEFAULT_GROQ_MODEL);

    public boolean isConfigured() {
        return AppConfig.get("GROQ_API_KEY").isPresent();
    }

    public String complete(List<Turn> turns) {
        String key = AppConfig.get("GROQ_API_KEY").orElseThrow(() -> new AppException(
                "A chave da Groq não foi configurada. Defina a variável GROQ_API_KEY e reinicie o aplicativo."));

        JsonArray messages = new JsonArray();
        for (Turn turn : turns) {
            JsonObject item = new JsonObject();
            item.addProperty("role", turn.role().apiName);
            item.addProperty("content", turn.content());
            messages.add(item);
        }
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.5);
        body.addProperty("max_tokens", 1500);

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            return parse(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (HttpConnectTimeoutException | ConnectException | UnknownHostException e) {
            throw new AppException("Sem conexão com a internet ou com a API da Groq. Verifique sua rede.", e);
        } catch (HttpTimeoutException e) {
            throw new AppException("A IA demorou demais para responder. Tente novamente.", e);
        } catch (IOException e) {
            throw new AppException("Não foi possível se comunicar com a API da Groq. Verifique sua internet.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("O envio da mensagem foi interrompido.", e);
        }
    }

    private String parse(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new AppException("A chave da Groq (GROQ_API_KEY) é inválida ou foi revogada.");
        }
        if (status == 404) {
            throw new AppException("O modelo \"" + model + "\" não está disponível na Groq. Ajuste GROQ_MODEL.");
        }
        if (status == 429) {
            throw new AppException("Limite de uso da Groq atingido. Aguarde alguns instantes e tente novamente.");
        }
        if (status >= 500) {
            throw new AppException("A API da Groq está indisponível no momento (erro " + status + "). Tente mais tarde.");
        }
        if (status != 200) {
            throw new AppException("A API da Groq recusou a requisição (erro " + status + ").",
                    new IllegalStateException(response.body()));
        }
        try {
            JsonElement content = JsonParser.parseString(response.body()).getAsJsonObject()
                    .getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content");
            String text = content == null || content.isJsonNull() ? "" : content.getAsString().trim();
            if (text.isEmpty()) {
                throw new AppException("A IA retornou uma resposta vazia. Tente reformular a pergunta.");
            }
            return text;
        } catch (JsonParseException | IllegalStateException | NullPointerException | IndexOutOfBoundsException e) {
            throw new AppException("A resposta da Groq veio em um formato inesperado.", e);
        }
    }
}
