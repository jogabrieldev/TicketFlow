package com.ticketApi.payment.exception;

import java.util.UUID;

public class PaymentUnavailableException extends RuntimeException {
    public PaymentUnavailableException(UUID pedidoId) {
        super("O pedido não está disponível para pagamento: " + pedidoId);
    }
}
