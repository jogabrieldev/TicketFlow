package com.ticketApi.order.exception;

import java.util.UUID;

public class ReservationUnavailableForOrderException extends RuntimeException {

    public ReservationUnavailableForOrderException(UUID reservaId) {
        super("A reserva " + reservaId + " não está disponível para criação de pedido");
    }
}
