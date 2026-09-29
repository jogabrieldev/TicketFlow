package com.ticketApi.payment.dto;

import com.ticketApi.payment.entity.Payment;
import com.ticketApi.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID pedidoId,
        PaymentStatus status,
        BigDecimal valor,
        String referenciaGateway,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    public static PaymentResponse de(Payment pagamento) {
        return new PaymentResponse(
                pagamento.obterId(),
                pagamento.obterPedido().obterId(),
                pagamento.obterStatus(),
                pagamento.obterValor(),
                pagamento.obterReferenciaGateway(),
                pagamento.obterCriadoEm(),
                pagamento.obterAtualizadoEm()
        );
    }
}
