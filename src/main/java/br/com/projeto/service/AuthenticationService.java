package br.com.projeto.service;

import br.com.projeto.exception.AuthenticationException;
import br.com.projeto.exception.ValidationException;
import br.com.projeto.model.User;
import br.com.projeto.util.PasswordHasher;
import br.com.projeto.util.Validator;

/** Cadastro, login e logout. */
public class AuthenticationService {

    private final UserService userService;
    private final SessionManager session;

    public AuthenticationService(UserService userService, SessionManager session) {
        this.userService = userService;
        this.session = session;
    }

    public User register(String name, String email, String password, String confirmation) {
        String validName = Validator.name(name);
        String validEmail = Validator.email(email);
        Validator.newPassword(password, confirmation);

        if (userService.emailExists(validEmail)) {
            throw new ValidationException("Já existe uma conta cadastrada com este e-mail.");
        }
        return userService.create(validName, validEmail, password);
    }

    public User login(String email, String password) {
        String validEmail = Validator.email(email);
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Informe a sua senha.");
        }
        User user = userService.findByEmail(validEmail)
                .orElseThrow(() -> new AuthenticationException("Não existe usuário cadastrado com este e-mail."));
        if (!PasswordHasher.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Senha incorreta. Tente novamente ou use \"Esqueci minha senha\".");
        }
        session.start(user);
        return user;
    }

    public void logout() {
        session.end();
    }
}
