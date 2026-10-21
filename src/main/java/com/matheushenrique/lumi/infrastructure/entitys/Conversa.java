package com.matheushenrique.lumi.infrastructure.entitys;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "conversations")
@CompoundIndex(def = "{'usuarioId': 1, 'atualizadoEm': -1}")
public class Conversa {

    public static final String TITULO_PADRAO = "Nova conversa";

    @Id
    private String id;

    private String usuarioId; // dono da conversa

    private String titulo;

    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;
}
