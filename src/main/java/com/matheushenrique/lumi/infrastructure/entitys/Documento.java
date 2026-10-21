package com.matheushenrique.lumi.infrastructure.entitys;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "documents")
public class Documento {

    @Id
    private String id;

    private String donoId; // null = documento público (exemplo da base)

    private String titulo;

    private String conteudo;

    @Indexed
    @Builder.Default
    private List<String> palavrasChave = new ArrayList<>();

    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

    public boolean isPublico() {
        return donoId == null;
    }
}
