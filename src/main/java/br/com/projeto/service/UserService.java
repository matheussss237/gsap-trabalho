package br.com.projeto.service;

import br.com.projeto.exception.AuthenticationException;
import br.com.projeto.exception.NotFoundException;
import br.com.projeto.model.User;
import br.com.projeto.repository.UserRepository;
import br.com.projeto.util.PasswordHasher;
import br.com.projeto.util.Validator;

import java.time.Instant;
import java.util.Optional;

/** Regras relacionadas aos dados do usuário. */
public class UserService {

    private final UserRepository userRepository;
    private final SessionManager session;

    public UserService(UserRepository userRepository, SessionManager session) {
        this.userRepository = userRepository;
        this.session = session;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(Validator.email(email));
    }

    public boolean emailExists(String normalizedEmail) {
        return userRepository.existsByEmail(normalizedEmail);
    }

    public User create(String name, String normalizedEmail, String plainPassword) {
        return userRepository.save(new User(name, normalizedEmail, PasswordHasher.hash(plainPassword)));
    }

    public void setPassword(String userId, String plainPassword) {
        userRepository.updatePassword(userId, PasswordHasher.hash(plainPassword), Instant.now());
    }

    /** Troca de senha do usuário logado (exige a senha atual). */
    public void changePassword(String currentPassword, String newPassword, String confirmation) {
        User user = userRepository.findById(session.requireUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        if (!PasswordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("A senha atual está incorreta.");
        }
        Validator.newPassword(newPassword, confirmation);
        setPassword(user.getId(), newPassword);
    }
}
