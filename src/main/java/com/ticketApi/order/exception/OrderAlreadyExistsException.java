package com.ticketApi.order.exception;

import java.util.UUID;

public class OrderAlreadyExistsException extends RuntimeException {

    public OrderAlreadyExistsException(UUID reservaId) {
        super("Já existe um pedido para a reserva " + reservaId);
    }
}
