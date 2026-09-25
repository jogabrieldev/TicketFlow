package com.ticketApi.user.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Já existe um usuário cadastrado com o e-mail: " + email);
    }
}
