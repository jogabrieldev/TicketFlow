package com.ticketApi.ticket.exception;

import java.util.UUID;

public class TicketBatchNotFoundException extends RuntimeException {

    public TicketBatchNotFoundException(UUID loteId) {
        super("Lote de ingressos não encontrado: " + loteId);
    }
}
