package com.ticketApi.shared.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByUsuarioIdAndOperacaoAndChave(
            UUID usuarioId,
            IdempotencyOperation operacao,
            String chave
    );
}
