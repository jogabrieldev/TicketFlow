package com.ticketApi.ticket.repository;

import com.ticketApi.ticket.entity.TicketBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TicketBatchRepository extends JpaRepository<TicketBatch, UUID> {

    @Query("SELECT lote FROM TicketBatch lote WHERE lote.evento.id = :eventoId")
    Page<TicketBatch> buscarPorEventoId(@Param("eventoId") UUID eventoId, Pageable paginacao);
}
