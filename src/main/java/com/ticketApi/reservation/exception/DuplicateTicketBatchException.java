package com.ticketApi.reservation.exception;

import java.util.UUID;

public class DuplicateTicketBatchException extends RuntimeException {

    public DuplicateTicketBatchException(UUID loteId) {
        super("O lote foi informado mais de uma vez na reserva: " + loteId);
    }
}
