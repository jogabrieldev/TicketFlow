package com.ticketApi.payment.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID pagamentoId) {
        super("Pagamento não encontrado: " + pagamentoId);
    }
}
