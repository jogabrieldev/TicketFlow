package com.ticketApi.shared.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idempotency_records_scope",
                columnNames = {"user_id", "operation", "idempotency_key"}
        )
)
public class IdempotencyRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 40, updatable = false)
    private IdempotencyOperation operacao;

    @Column(name = "idempotency_key", nullable = false, length = 255, updatable = false)
    private String chave;

    @Column(name = "request_hash", nullable = false, length = 64, updatable = false)
    private String hashDaRequisicao;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID recursoId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(
            UUID usuarioId,
            IdempotencyOperation operacao,
            String chave,
            String hashDaRequisicao,
            UUID recursoId
    ) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.operacao = operacao;
        this.chave = chave;
        this.hashDaRequisicao = hashDaRequisicao;
        this.recursoId = recursoId;
    }

    public String obterHashDaRequisicao() {
        return hashDaRequisicao;
    }

    public UUID obterRecursoId() {
        return recursoId;
    }
}
