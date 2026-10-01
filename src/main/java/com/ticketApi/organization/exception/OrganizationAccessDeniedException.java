package com.ticketApi.organization.exception;

import java.util.UUID;

public class OrganizationAccessDeniedException extends RuntimeException {

    public OrganizationAccessDeniedException(UUID organizacaoId) {
        super("O usuário autenticado não possui permissão para acessar a organização: " + organizacaoId);
    }
}
