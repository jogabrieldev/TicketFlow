package com.ticketApi.ticket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateTicketBatchRequest(
        @NotBlank(message = "O nome do lote é obrigatório")
        @Size(max = 255, message = "O nome do lote deve ter no máximo 255 caracteres")
        String nome,

        @NotNull(message = "O preço do lote é obrigatório")
        @DecimalMin(value = "0.00", inclusive = false, message = "O preço do lote deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O preço do lote deve ter até 10 dígitos inteiros e duas casas decimais")
        BigDecimal preco,

        @NotNull(message = "A quantidade total do lote é obrigatória")
        @Positive(message = "A quantidade total do lote deve ser maior que zero")
        Integer quantidadeTotal
) {
}
