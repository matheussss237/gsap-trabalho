package br.com.projeto.service;

import java.util.List;

/** Contrato de um provedor de IA. A implementação real é {@link GroqService}. */
public interface AiClient {

    String complete(List<AiMessage> messages);

    boolean isConfigured();

    String modelName();
}
