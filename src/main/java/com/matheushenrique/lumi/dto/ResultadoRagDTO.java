package com.matheushenrique.lumi.dto;

import com.matheushenrique.lumi.infrastructure.entitys.Documento;

import java.util.List;

// resultado da busca do RAG: palavras-chave da pergunta e documentos encontrados (já ordenados)
public record ResultadoRagDTO(List<String> palavrasChave, List<DocumentoEncontrado> documentos) {

    public record DocumentoEncontrado(Documento documento, int pontuacao, List<String> termosEncontrados) {
    }

    public boolean temContexto() {
        return !documentos.isEmpty();
    }

    public List<String> titulos() {
        return documentos.stream().map(d -> d.documento().getTitulo()).toList();
    }
}
