package com.ticketApi.organization.exception;

public class OrganizationManagementDeniedException extends RuntimeException {

    public OrganizationManagementDeniedException() {
        super("O usuário autenticado não possui permissão para administrar organizações");
    }
}
