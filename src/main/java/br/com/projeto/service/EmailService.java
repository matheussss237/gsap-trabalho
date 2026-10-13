package br.com.projeto.service;

/** Envio de e-mails (usado pela recuperação de senha). */
public interface EmailService {

    void send(String to, String subject, String body);
}
