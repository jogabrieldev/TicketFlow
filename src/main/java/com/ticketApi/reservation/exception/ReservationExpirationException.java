package com.ticketApi.reservation.exception;

import java.util.UUID;

public class ReservationExpirationException extends RuntimeException {

    public ReservationExpirationException(UUID reservaId, String detalhe) {
        super("Não foi possível expirar a reserva " + reservaId + ": " + detalhe);
    }
}
