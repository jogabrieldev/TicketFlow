package com.ticketApi.reservation.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ReservationPageResponse(
        List<ReservationResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    public static ReservationPageResponse de(Page<ReservationResponse> reservas) {
        return new ReservationPageResponse(
                reservas.getContent(),
                reservas.getNumber(),
                reservas.getSize(),
                reservas.getTotalElements(),
                reservas.getTotalPages()
        );
    }
}
