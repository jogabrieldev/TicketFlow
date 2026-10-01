package com.ticketApi.organization.exception;

public class OrganizationOwnerAlreadyExistsException extends RuntimeException {

    public OrganizationOwnerAlreadyExistsException(String email) {
        super("O usuário já é proprietário de uma organização: " + email);
    }
}
