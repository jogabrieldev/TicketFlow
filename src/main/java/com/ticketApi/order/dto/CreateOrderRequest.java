package com.ticketApi.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "O identificador da reserva é obrigatório")
        UUID reservaId
) {
}
