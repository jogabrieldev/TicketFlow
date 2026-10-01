package com.ticketApi.organization.dto;

import com.ticketApi.organization.entity.Organization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String nomeFantasia,
        String razaoSocial,
        String cnpj,
        String email,
        String telefone,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        String cep,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {

    public static OrganizationResponse de(Organization organizacao) {
        return new OrganizationResponse(
                organizacao.obterId(),
                organizacao.obterNomeFantasia(),
                organizacao.obterRazaoSocial(),
                organizacao.obterCnpj(),
                organizacao.obterEmail(),
                organizacao.obterTelefone(),
                organizacao.obterLogradouro(),
                organizacao.obterNumero(),
                organizacao.obterComplemento(),
                organizacao.obterBairro(),
                organizacao.obterCidade(),
                organizacao.obterEstado(),
                organizacao.obterCep(),
                organizacao.obterCriadoEm(),
                organizacao.obterAtualizadoEm()
        );
    }
}
