package com.ticketApi.ticket.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record TicketBatchPageResponse(
        List<TicketBatchResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    public static TicketBatchPageResponse de(Page<TicketBatchResponse> lotes) {
        return new TicketBatchPageResponse(
                lotes.getContent(),
                lotes.getNumber(),
                lotes.getSize(),
                lotes.getTotalElements(),
                lotes.getTotalPages()
        );
    }
}
