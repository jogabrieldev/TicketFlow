package com.ticketApi.order.dto;

import com.ticketApi.order.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID loteId,
        String nomeLote,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {

    public static OrderItemResponse de(OrderItem item) {
        return new OrderItemResponse(
                item.obterLote().obterId(),
                item.obterLote().obterNome(),
                item.obterQuantidade(),
                item.obterPrecoUnitario(),
                item.calcularSubtotal()
        );
    }
}
