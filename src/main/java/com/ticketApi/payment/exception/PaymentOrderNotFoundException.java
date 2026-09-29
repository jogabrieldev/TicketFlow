package com.ticketApi.payment.exception;

import java.util.UUID;

public class PaymentOrderNotFoundException extends RuntimeException {
    public PaymentOrderNotFoundException(UUID pedidoId) {
        super("Pedido não encontrado para pagamento: " + pedidoId);
    }
}
