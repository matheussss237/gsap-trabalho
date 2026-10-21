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
@Document(collection = "password_reset_tokens")
public class TokenRecuperacao {

    @Id
    private String id;

    @Indexed
    private String usuarioId;

    private String codigoHash; // só o SHA-256 do código fica salvo

    private LocalDateTime criadoEm;

    @Indexed(expireAfterSeconds = 0) // o MongoDB apaga sozinho quando vence
    private LocalDateTime expiraEm;

    private boolean usado;

    private int tentativas;
}
