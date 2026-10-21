package com.matheushenrique.lumi.infrastructure.repository;

import com.matheushenrique.lumi.infrastructure.entitys.TokenRecuperacao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRecuperacaoRepository extends MongoRepository<TokenRecuperacao, String> {

    Optional<TokenRecuperacao> findFirstByUsuarioIdAndUsadoFalseOrderByCriadoEmDesc(String usuarioId);

    List<TokenRecuperacao> findByUsuarioIdAndUsadoFalse(String usuarioId);
}
