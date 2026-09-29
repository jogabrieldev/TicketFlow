package com.ticketApi.payment.gateway;

import com.ticketApi.payment.entity.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class FakePaymentGateway implements PaymentGateway {

    static final String TOKEN_APROVADO = "tok_aprovado";
    static final String TOKEN_PROCESSANDO = "tok_processando";

    @Override
    public PaymentGatewayResult processar(UUID pedidoId, BigDecimal valor, String tokenPagamento) {
        PaymentStatus status = switch (tokenPagamento) {
            case TOKEN_APROVADO -> PaymentStatus.APROVADO;
            case TOKEN_PROCESSANDO -> PaymentStatus.PROCESSANDO;
            default -> PaymentStatus.RECUSADO;
        };
        return new PaymentGatewayResult(status, "fake_" + UUID.randomUUID());
    }
}
