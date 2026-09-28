package com.ticketApi.order.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record OrderPageResponse(
        List<OrderResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    public static OrderPageResponse de(Page<OrderResponse> pedidos) {
        return new OrderPageResponse(
                pedidos.getContent(),
                pedidos.getNumber(),
                pedidos.getSize(),
                pedidos.getTotalElements(),
                pedidos.getTotalPages()
        );
    }
}
