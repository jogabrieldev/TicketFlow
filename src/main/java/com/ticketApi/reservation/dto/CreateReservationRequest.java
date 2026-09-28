package com.ticketApi.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateReservationRequest(
        @NotNull(message = "Os itens da reserva são obrigatórios")
        @NotEmpty(message = "A reserva deve possuir ao menos um item")
        List<@Valid CreateReservationItemRequest> itens
) {
}
