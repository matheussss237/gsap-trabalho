package com.matheushenrique.lumi.infrastructure.entitys;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
@CompoundIndex(def = "{'conversaId': 1, 'usuarioId': 1, 'criadoEm': 1}")
public class Mensagem {

    // quem escreveu a mensagem; "valor" é o nome que a API da Groq espera
    public enum Autor {
        USUARIO("user"),
        ASSISTENTE("assistant"),
        SISTEMA("system");

        private final String valor;

        Autor(String valor) {
            this.valor = valor;
        }

        public String getValor() {
            return valor;
        }
    }

    @Id
    private String id;

    private String conversaId;

    private String usuarioId;

    private Autor autor;

    private String conteudo;

    @Builder.Default
    private List<String> fontes = new ArrayList<>(); // documentos usados pelo RAG

    private LocalDateTime criadoEm;
}
