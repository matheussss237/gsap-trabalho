package com.matheushenrique.lumi.infrastructure.repository;

import com.matheushenrique.lumi.infrastructure.entitys.Mensagem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MensagemRepository extends MongoRepository<Mensagem, String> {

    List<Mensagem> findByConversaIdAndUsuarioIdOrderByCriadoEmAsc(String conversaId, String usuarioId);

    long deleteByConversaIdAndUsuarioId(String conversaId, String usuarioId);
}
