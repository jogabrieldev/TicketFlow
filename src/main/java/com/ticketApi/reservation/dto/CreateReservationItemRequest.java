package com.ticketApi.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateReservationItemRequest(
        @NotNull(message = "O identificador do lote é obrigatório")
        UUID loteId,

        @NotNull(message = "A quantidade do item é obrigatória")
        @Positive(message = "A quantidade do item deve ser maior que zero")
        Integer quantidade
) {
}
