package com.ticketApi.ticket.dto;

import com.ticketApi.ticket.entity.TicketBatch;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketBatchResponse(
        UUID id,
        UUID eventoId,
        String nome,
        BigDecimal preco,
        int quantidadeTotal,
        int quantidadeDisponivel,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {

    public static TicketBatchResponse de(TicketBatch lote) {
        return new TicketBatchResponse(
                lote.obterId(),
                lote.obterEvento().obterId(),
                lote.obterNome(),
                lote.obterPreco(),
                lote.obterQuantidadeTotal(),
                lote.obterQuantidadeDisponivel(),
                lote.obterCriadoEm(),
                lote.obterAtualizadoEm()
        );
    }
}
