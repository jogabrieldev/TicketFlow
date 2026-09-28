package com.ticketApi.reservation.exception;

public class EmptyReservationException extends RuntimeException {

    public EmptyReservationException() {
        super("A reserva deve possuir ao menos um item");
    }
}
