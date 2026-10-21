package com.matheushenrique.lumi.dto;

import com.matheushenrique.lumi.infrastructure.entitys.Conversa;
import com.matheushenrique.lumi.infrastructure.entitys.Mensagem;

// o que o ChatService devolve depois de enviar uma pergunta
public record RespostaChatDTO(Mensagem pergunta, Mensagem resposta, Conversa conversa) {
}
