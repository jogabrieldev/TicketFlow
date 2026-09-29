package com.ticketApi.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull(message = "O identificador do pedido é obrigatório")
        UUID pedidoId,

        @NotBlank(message = "O token de pagamento é obrigatório")
        @Size(max = 100, message = "O token de pagamento deve ter no máximo 100 caracteres")
        String tokenPagamento
) {
}
