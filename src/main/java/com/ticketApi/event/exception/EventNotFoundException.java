package com.ticketApi.event.exception;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(UUID eventoId) {
        super("Evento não encontrado: " + eventoId);
    }
}
