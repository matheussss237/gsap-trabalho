package br.com.projeto.repository.mongo;

import br.com.projeto.exception.DatabaseException;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

/** Utilidades comuns às implementações MongoDB dos repositórios. */
final class MongoSupport {

    private MongoSupport() {
    }

    /** Executa a operação convertendo falhas do driver em mensagens amigáveis. */
    static <T> T execute(String action, Supplier<T> operation) {
        try {
            return operation.get();
        } catch (MongoTimeoutException e) {
            throw new DatabaseException("O banco de dados não respondeu ao " + action
                    + ". Verifique sua conexão com a internet e o MongoDB Atlas.", e);
        } catch (MongoException e) {
            throw new DatabaseException("Ocorreu um erro no banco de dados ao " + action + ".", e);
        }
    }

    static void run(String action, Runnable operation) {
        execute(action, () -> {
            operation.run();
            return null;
        });
    }

    /** Converte o id textual em ObjectId; ids inválidos simplesmente não existem. */
    static Optional<ObjectId> objectId(String id) {
        return id != null && ObjectId.isValid(id) ? Optional.of(new ObjectId(id)) : Optional.empty();
    }

    static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
