package br.com.projeto.support;

import br.com.projeto.config.AppContext;

import java.util.Optional;

/** AppContext completo com repositórios em memória e IA falsa. */
public class TestContext {

    public final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    public final InMemoryRepositories.Conversations conversations = new InMemoryRepositories.Conversations();
    public final InMemoryRepositories.Messages messages = new InMemoryRepositories.Messages();
    public final InMemoryRepositories.Documents documents = new InMemoryRepositories.Documents();
    public final InMemoryRepositories.ResetTokens tokens = new InMemoryRepositories.ResetTokens();
    public final FakeAiClient ai = new FakeAiClient();
    public final AppContext app = new AppContext(users, conversations, messages, documents, tokens, ai, Optional.empty());
}
