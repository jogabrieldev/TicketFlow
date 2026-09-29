package com.ticketApi.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    PaymentGatewayResult processar(UUID pedidoId, BigDecimal valor, String tokenPagamento);
}
