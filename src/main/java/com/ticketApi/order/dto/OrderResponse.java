package com.ticketApi.order.dto;

import com.ticketApi.order.entity.Order;
import com.ticketApi.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID reservaId,
        OrderStatus status,
        BigDecimal valorTotal,
        OffsetDateTime limitePagamento,
        List<OrderItemResponse> itens,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {

    public static OrderResponse de(Order pedido) {
        return new OrderResponse(
                pedido.obterId(),
                pedido.obterReserva().obterId(),
                pedido.obterStatus(),
                pedido.obterValorTotal(),
                pedido.obterLimitePagamento(),
                pedido.obterItens().stream().map(OrderItemResponse::de).toList(),
                pedido.obterCriadoEm(),
                pedido.obterAtualizadoEm()
        );
    }
}
