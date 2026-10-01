package com.ticketApi.organization.exception;

public class CnpjAlreadyRegisteredException extends RuntimeException {

    public CnpjAlreadyRegisteredException(String cnpj) {
        super("Já existe uma organização cadastrada com o CNPJ: " + cnpj);
    }
}
