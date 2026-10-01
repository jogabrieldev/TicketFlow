package com.ticketApi.organization.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record OrganizationPageResponse(
        List<OrganizationResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    public static OrganizationPageResponse de(Page<OrganizationResponse> organizacoes) {
        return new OrganizationPageResponse(
                organizacoes.getContent(),
                organizacoes.getNumber(),
                organizacoes.getSize(),
                organizacoes.getTotalElements(),
                organizacoes.getTotalPages()
        );
    }
}
