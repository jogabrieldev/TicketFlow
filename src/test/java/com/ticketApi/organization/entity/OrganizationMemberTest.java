package com.ticketApi.organization.entity;

import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationMemberTest {

    @Test
    void deveCriarProprietarioDaOrganizacao() {
        Organization organizacao = criarOrganizacao();
        User usuario = criarUsuario();

        OrganizationMember membro = new OrganizationMember(
                organizacao,
                usuario,
                OrganizationMemberRole.PROPRIETARIO
        );

        assertThat(membro.obterId()).isNotNull();
        assertThat(membro.obterOrganizacao()).isSameAs(organizacao);
        assertThat(membro.obterUsuario()).isSameAs(usuario);
        assertThat(membro.obterPapel()).isEqualTo(OrganizationMemberRole.PROPRIETARIO);
    }

    @Test
    void deveRejeitarMembroSemOrganizacao() {
        assertThatThrownBy(() -> new OrganizationMember(
                null,
                criarUsuario(),
                OrganizationMemberRole.PROPRIETARIO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A organização do membro é obrigatória");
    }

    @Test
    void deveRejeitarMembroSemUsuario() {
        assertThatThrownBy(() -> new OrganizationMember(
                criarOrganizacao(),
                null,
                OrganizationMemberRole.PROPRIETARIO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O usuário do membro é obrigatório");
    }

    @Test
    void deveRejeitarClienteComoMembroDaOrganizacao() {
        User cliente = new User("Cliente", "cliente@exemplo.com", "hash-seguro", UserRole.CLIENTE);

        assertThatThrownBy(() -> new OrganizationMember(
                criarOrganizacao(),
                cliente,
                OrganizationMemberRole.PROPRIETARIO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Somente administradores podem pertencer a uma organização");
    }

    private Organization criarOrganizacao() {
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

    private User criarUsuario() {
        return new User("Maria Silva", "maria@exemplo.com", "hash-seguro", UserRole.ADMINISTRADOR);
    }
}
