package com.ticketApi.organization.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationTest {

    @Test
    void deveCriarOrganizacaoNormalizandoDados() {
        Organization organizacao = criarOrganizacao();

        assertThat(organizacao.obterId()).isNotNull();
        assertThat(organizacao.obterNomeFantasia()).isEqualTo("Ticket Flow");
        assertThat(organizacao.obterRazaoSocial()).isEqualTo("Ticket Flow Tecnologia Ltda");
        assertThat(organizacao.obterCnpj()).isEqualTo("11222333000181");
        assertThat(organizacao.obterEmail()).isEqualTo("contato@ticketflow.com.br");
        assertThat(organizacao.obterTelefone()).isEqualTo("11987654321");
        assertThat(organizacao.obterEstado()).isEqualTo("SP");
        assertThat(organizacao.obterCep()).isEqualTo("01310100");
        assertThat(organizacao.obterComplemento()).isNull();
    }

    @Test
    void deveRejeitarCnpjInvalido() {
        assertThatThrownBy(() -> new Organization(
                "Ticket Flow",
                "Ticket Flow Tecnologia Ltda",
                "11.111.111/1111-11",
                "contato@ticketflow.com.br",
                "(11) 98765-4321",
                "Avenida Paulista",
                "1000",
                null,
                "Bela Vista",
                "São Paulo",
                "SP",
                "01310-100"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O CNPJ da organização é inválido");
    }

    @Test
    void deveRejeitarTelefoneInvalido() {
        assertThatThrownBy(() -> new Organization(
                "Ticket Flow",
                "Ticket Flow Tecnologia Ltda",
                "11.222.333/0001-81",
                "contato@ticketflow.com.br",
                "123",
                "Avenida Paulista",
                "1000",
                null,
                "Bela Vista",
                "São Paulo",
                "SP",
                "01310-100"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O telefone da organização deve possuir 10 ou 11 dígitos");
    }

    @Test
    void deveRejeitarEstadoInvalido() {
        assertThatThrownBy(() -> new Organization(
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
                "São Paulo",
                "01310-100"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O estado da organização deve possuir duas letras");
    }

    private Organization criarOrganizacao() {
        return new Organization(
                " Ticket Flow ",
                " Ticket Flow Tecnologia Ltda ",
                "11.222.333/0001-81",
                " Contato@TicketFlow.com.br ",
                "(11) 98765-4321",
                " Avenida Paulista ",
                " 1000 ",
                " ",
                " Bela Vista ",
                " São Paulo ",
                "sp",
                "01310-100"
        );
    }
}
