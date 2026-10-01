package com.ticketApi.organization;

import com.ticketApi.organization.entity.Organization;

public final class OrganizationTestFactory {

    private OrganizationTestFactory() {
    }

    public static Organization criarOrganizacao() {
        return new Organization(
                "Ticket Flow",
                "Ticket Flow Tecnologia Ltda",
                "11.222.333/0001-81",
                "contato@ticketflow.com.br",
                "(11) 98765-4321",
                "Avenida Paulista",
                "1000",
                null,
                "Bela Vista",
                "São Paulo",
                "SP",
                "01310-100"
        );
    }
}
