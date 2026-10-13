package br.com.projeto.support;

import br.com.projeto.exception.AiServiceException;
import br.com.projeto.service.AiClient;
import br.com.projeto.service.AiMessage;

import java.util.ArrayList;
import java.util.List;

/** IA falsa: devolve uma resposta fixa e registra o prompt recebido. */
public class FakeAiClient implements AiClient {

    public final List<List<AiMessage>> calls = new ArrayList<>();
    public boolean fail;
    public String answer = "Resposta de teste.";

    @Override
    public String complete(List<AiMessage> messages) {
        calls.add(messages);
        if (fail) {
            throw new AiServiceException("A API da Groq está indisponível no momento.");
        }
        return answer;
    }

    @Override public boolean isConfigured() { return true; }
    @Override public String modelName() { return "fake-model"; }

    public List<AiMessage> lastPrompt() {
        return calls.get(calls.size() - 1);
    }
}
