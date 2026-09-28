package com.ticketApi.ticket.exception;

import java.util.UUID;

public class InsufficientTicketAvailabilityException extends RuntimeException {

    public InsufficientTicketAvailabilityException(UUID loteId, int quantidadeSolicitada) {
        super("Quantidade indisponível para o lote " + loteId
                + ": solicitada=" + quantidadeSolicitada);
    }
}
