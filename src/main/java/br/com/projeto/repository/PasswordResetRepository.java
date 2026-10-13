package br.com.projeto.repository;

import br.com.projeto.model.PasswordResetToken;

import java.util.Optional;

/** Acesso aos dados da coleção "password_reset_tokens". */
public interface PasswordResetRepository {

    PasswordResetToken save(PasswordResetToken token);

    /** Token mais recente ainda não utilizado do usuário. */
    Optional<PasswordResetToken> findLatestUnused(String userId);

    void update(PasswordResetToken token);

    void invalidateAll(String userId);
}
