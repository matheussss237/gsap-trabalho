package br.com.projeto.service;

import br.com.projeto.model.User;
import br.com.projeto.repository.UserRepository;
import br.com.projeto.util.AppException;
import br.com.projeto.util.Security;

import java.util.Optional;

/**
 * Cadastro, login, logout e controle de sessão.
 * A sessão guarda em memória o usuário autenticado; os outros Services obtêm o dono
 * dos dados SEMPRE daqui, nunca de valores vindos da tela.
 */
public class AuthenticationService {

    private final UserRepository userRepository;
    private User currentUser;

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String name, String email, String password, String confirmation) {
        String validName = Security.name(name);
        String validEmail = Security.email(email);
        Security.newPassword(password, confirmation);
        if (userRepository.existsByEmail(validEmail)) {
            throw new AppException("Já existe uma conta cadastrada com este e-mail.");
        }
        return userRepository.save(new User(validName, validEmail, Security.hashPassword(password)));
    }

    public User login(String email, String password) {
        String validEmail = Security.email(email);
        if (password == null || password.isEmpty()) {
            throw new AppException("Informe a sua senha.");
        }
        User user = userRepository.findByEmail(validEmail)
                .orElseThrow(() -> new AppException("Não existe usuário cadastrado com este e-mail."));
        if (!Security.checkPassword(password, user.getPasswordHash())) {
            throw new AppException("Senha incorreta. Tente novamente ou use \"Esqueci minha senha\".");
        }
        synchronized (this) {
            currentUser = user;
        }
        return user;
    }

    public synchronized void logout() {
        currentUser = null;
    }

    public synchronized Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    /** Usuário logado ou erro: impede qualquer operação sem login. */
    public synchronized User requireUser() {
        if (currentUser == null) {
            throw new AppException("Sua sessão foi encerrada. Faça login novamente.");
        }
        return currentUser;
    }

    public String requireUserId() {
        return requireUser().getId();
    }

    // usados pela recuperação de senha
    Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(Security.email(email));
    }

    void changePassword(String userId, String newPassword) {
        userRepository.updatePassword(userId, Security.hashPassword(newPassword));
    }
}
