package com.ticketApi.payment.gateway;

import com.ticketApi.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FakePaymentGatewayTest {

    private final FakePaymentGateway gateway = new FakePaymentGateway();

    @Test
    void deveSimularOsTresResultados() {
        UUID pedidoId = UUID.randomUUID();

        assertThat(gateway.processar(pedidoId, BigDecimal.TEN, "tok_aprovado").status())
                .isEqualTo(PaymentStatus.APROVADO);
        assertThat(gateway.processar(pedidoId, BigDecimal.TEN, "tok_processando").status())
                .isEqualTo(PaymentStatus.PROCESSANDO);
        assertThat(gateway.processar(pedidoId, BigDecimal.TEN, "tok_recusado").status())
                .isEqualTo(PaymentStatus.RECUSADO);
    }
}
