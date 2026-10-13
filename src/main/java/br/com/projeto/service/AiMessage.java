package br.com.projeto.service;

import br.com.projeto.model.MessageRole;

/** Mensagem no formato enviado ao modelo de IA (papel + texto). */
public record AiMessage(MessageRole role, String content) {
}
