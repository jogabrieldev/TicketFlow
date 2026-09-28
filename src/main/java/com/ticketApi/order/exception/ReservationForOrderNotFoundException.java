package com.ticketApi.order.exception;

import java.util.UUID;

public class ReservationForOrderNotFoundException extends RuntimeException {

    public ReservationForOrderNotFoundException(UUID reservaId) {
        super("Reserva não encontrada: " + reservaId);
    }
}
