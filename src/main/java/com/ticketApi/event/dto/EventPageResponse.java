package com.ticketApi.event.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record EventPageResponse(
        List<EventResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    public static EventPageResponse de(Page<EventResponse> eventos) {
        return new EventPageResponse(
                eventos.getContent(),
                eventos.getNumber(),
                eventos.getSize(),
                eventos.getTotalElements(),
                eventos.getTotalPages()
        );
    }
}
