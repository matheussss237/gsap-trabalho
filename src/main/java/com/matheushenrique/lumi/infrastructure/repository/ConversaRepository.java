package com.matheushenrique.lumi.infrastructure.repository;

import com.matheushenrique.lumi.infrastructure.entitys.Conversa;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

// sempre busca pelo id da conversa E pelo id do usuário: um usuário nunca acessa a conversa de outro
public interface ConversaRepository extends MongoRepository<Conversa, String> {

    List<Conversa> findByUsuarioIdOrderByAtualizadoEmDesc(String usuarioId);

    Optional<Conversa> findByIdAndUsuarioId(String id, String usuarioId);

    long deleteByIdAndUsuarioId(String id, String usuarioId);
}
