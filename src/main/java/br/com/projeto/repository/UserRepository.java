package br.com.projeto.repository;

import br.com.projeto.model.User;

import java.time.Instant;
import java.util.Optional;

/** Acesso aos dados da coleção "users". */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void updatePassword(String userId, String passwordHash, Instant updatedAt);
}
