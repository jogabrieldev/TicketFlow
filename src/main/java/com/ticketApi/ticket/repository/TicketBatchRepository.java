package com.ticketApi.ticket.repository;

import com.ticketApi.ticket.entity.TicketBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface TicketBatchRepository extends JpaRepository<TicketBatch, UUID> {

    @Query("SELECT lote FROM TicketBatch lote WHERE lote.evento.id = :eventoId")
    Page<TicketBatch> buscarPorEventoId(@Param("eventoId") UUID eventoId, Pageable paginacao);

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("""
            UPDATE TicketBatch lote
               SET lote.quantidadeDisponivel = lote.quantidadeDisponivel - :quantidade,
                   lote.atualizadoEm = :agora
             WHERE lote.id = :loteId
               AND lote.quantidadeDisponivel >= :quantidade
            """)
    int reservarSeDisponivel(
            @Param("loteId") UUID loteId,
            @Param("quantidade") int quantidade,
            @Param("agora") OffsetDateTime agora
    );

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("""
            UPDATE TicketBatch lote
               SET lote.quantidadeDisponivel = lote.quantidadeDisponivel + :quantidade,
                   lote.atualizadoEm = :agora
             WHERE lote.id = :loteId
               AND lote.quantidadeDisponivel + :quantidade <= lote.quantidadeTotal
            """)
    int restaurarDisponibilidade(
            @Param("loteId") UUID loteId,
            @Param("quantidade") int quantidade,
            @Param("agora") OffsetDateTime agora
    );
}
