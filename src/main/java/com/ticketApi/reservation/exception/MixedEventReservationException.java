package com.ticketApi.reservation.exception;

public class MixedEventReservationException extends RuntimeException {

    public MixedEventReservationException() {
        super("Todos os lotes da reserva devem pertencer ao mesmo evento");
    }
}
