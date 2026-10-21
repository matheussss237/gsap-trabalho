package com.matheushenrique.lumi.infrastructure.repository;

import com.matheushenrique.lumi.infrastructure.entitys.Documento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface DocumentoRepository extends MongoRepository<Documento, String> {

    // documentos públicos ou do usuário que tenham pelo menos uma das palavras-chave
    @Query("{ 'palavrasChave': { $in: ?0 }, $or: [ { 'donoId': null }, { 'donoId': ?1 } ] }")
    List<Documento> buscarPorPalavrasChave(List<String> palavras, String usuarioId);

    @Query(value = "{ $or: [ { 'donoId': null }, { 'donoId': ?0 } ] }", sort = "{ 'titulo': 1 }")
    List<Documento> listarVisiveis(String usuarioId);

    long countByDonoIdIsNull();

    long deleteByIdAndDonoId(String id, String donoId);
}
