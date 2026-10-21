package com.matheushenrique.lumi.infrastructure.entitys;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class Usuario {

    @Id
    private String id;

    private String nome;

    @Indexed(unique = true)
    private String email;

    private String senha; // hash BCrypt, nunca a senha em texto puro

    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

    // primeiro nome, usado para a Lumi chamar o usuário pelo nome
    public String primeiroNome() {
        return nome == null || nome.isBlank() ? "" : nome.trim().split("\\s+")[0];
    }
}
