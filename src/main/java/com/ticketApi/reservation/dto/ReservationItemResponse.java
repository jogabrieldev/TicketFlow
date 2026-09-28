package com.ticketApi.reservation.dto;

import com.ticketApi.reservation.entity.ReservationItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ReservationItemResponse(
        UUID id,
        UUID loteId,
        String loteNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {

    public static ReservationItemResponse de(ReservationItem item) {
        BigDecimal subtotal = item.obterPrecoUnitario().multiply(BigDecimal.valueOf(item.obterQuantidade()));
        return new ReservationItemResponse(
                item.obterId(),
                item.obterLote().obterId(),
                item.obterLote().obterNome(),
                item.obterQuantidade(),
                item.obterPrecoUnitario(),
                subtotal
        );
    }
}
