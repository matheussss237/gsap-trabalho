package br.com.projeto.service;

import br.com.projeto.exception.AuthenticationException;
import br.com.projeto.model.User;

import java.time.Instant;
import java.util.Optional;

/**
 * Controle de sessão: guarda em memória o usuário autenticado.
 * Os Services obtêm o dono dos dados SEMPRE daqui, nunca de parâmetros vindos da tela.
 */
public class SessionManager {

    private User currentUser;
    private Instant startedAt;

    public synchronized void start(User user) {
        this.currentUser = user;
        this.startedAt = Instant.now();
    }

    public synchronized void end() {
        this.currentUser = null;
        this.startedAt = null;
    }

    public synchronized boolean isAuthenticated() {
        return currentUser != null;
    }

    public synchronized Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public synchronized Instant startedAt() {
        return startedAt;
    }

    /** Usuário autenticado ou erro: impede operações sem login. */
    public synchronized User requireUser() {
        if (currentUser == null) {
            throw new AuthenticationException("Sua sessão foi encerrada. Faça login novamente.");
        }
        return currentUser;
    }

    public String requireUserId() {
        return requireUser().getId();
    }
}
