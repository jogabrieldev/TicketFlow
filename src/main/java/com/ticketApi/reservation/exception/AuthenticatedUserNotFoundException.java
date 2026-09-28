package com.ticketApi.reservation.exception;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException(String email) {
        super("Usuário autenticado não encontrado: " + email);
    }
}
