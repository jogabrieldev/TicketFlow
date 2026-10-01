package com.ticketApi.organization.exception;

import java.util.UUID;

public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(UUID organizacaoId) {
        super("Organização não encontrada: " + organizacaoId);
    }
}
