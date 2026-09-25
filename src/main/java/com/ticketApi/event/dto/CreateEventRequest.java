package com.ticketApi.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateEventRequest(
        @NotBlank(message = "O nome do evento é obrigatório")
        @Size(max = 255, message = "O nome do evento deve ter no máximo 255 caracteres")
        String nome,

        String descricao,

        @NotBlank(message = "O local do evento é obrigatório")
        @Size(max = 255, message = "O local do evento deve ter no máximo 255 caracteres")
        String local,

        @NotNull(message = "A data de início do evento é obrigatória")
        OffsetDateTime inicioEm,

        @NotNull(message = "A data de término do evento é obrigatória")
        OffsetDateTime terminoEm
) {
}
