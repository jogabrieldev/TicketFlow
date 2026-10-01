package com.ticketApi.organization.repository;

import com.ticketApi.organization.entity.Organization;
import com.ticketApi.organization.entity.OrganizationMember;
import com.ticketApi.organization.entity.OrganizationMemberRole;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "ticketflow.reservation.expiration.enabled=false")
@Transactional
class OrganizationRepositoryTest {

    @Autowired
    private OrganizationRepository repositorioDeOrganizacoes;

    @Autowired
    private OrganizationMemberRepository repositorioDeMembros;

    @Autowired
    private UserRepository repositorioDeUsuarios;

    @Test
    void devePersistirEConsultarOrganizacaoDoProprietario() {
        User usuario = criarUsuario();
        Organization organizacao = repositorioDeOrganizacoes.saveAndFlush(criarOrganizacao("11222333000181"));
        repositorioDeMembros.saveAndFlush(new OrganizationMember(
                organizacao,
                usuario,
                OrganizationMemberRole.PROPRIETARIO
        ));

        assertThat(repositorioDeMembros.existePorUsuarioIdEPapel(
                usuario.obterId(), OrganizationMemberRole.PROPRIETARIO)).isTrue();
        assertThat(repositorioDeMembros.existePorOrganizacaoIdEUsuarioIdEPapel(
                organizacao.obterId(), usuario.obterId(), OrganizationMemberRole.PROPRIETARIO)).isTrue();

        var pagina = repositorioDeMembros.buscarPorUsuarioIdEPapel(
                usuario.obterId(),
                OrganizationMemberRole.PROPRIETARIO,
                PageRequest.of(0, 20, Sort.by("organizacao.nomeFantasia"))
        );
        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().getFirst().obterOrganizacao().obterId()).isEqualTo(organizacao.obterId());
    }

    @Test
    void deveImpedirMesmoUsuarioComoProprietarioDeDuasOrganizacoes() {
        User usuario = criarUsuario();
        Organization primeira = repositorioDeOrganizacoes.saveAndFlush(criarOrganizacao("11222333000181"));
        Organization segunda = repositorioDeOrganizacoes.saveAndFlush(criarOrganizacao("45723174000110"));
        repositorioDeMembros.saveAndFlush(new OrganizationMember(
                primeira,
                usuario,
                OrganizationMemberRole.PROPRIETARIO
        ));

        assertThatThrownBy(() -> repositorioDeMembros.saveAndFlush(new OrganizationMember(
                segunda,
                usuario,
                OrganizationMemberRole.PROPRIETARIO
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private User criarUsuario() {
        return repositorioDeUsuarios.saveAndFlush(new User(
                "Usuário do teste",
                "organizacao-" + UUID.randomUUID() + "@exemplo.com",
                "hash-seguro",
                UserRole.ADMINISTRADOR
        ));
    }

    private Organization criarOrganizacao(String cnpj) {
        return new Organization(
                "Organização do teste",
                "Organização do Teste Ltda",
                cnpj,
                "contato@organizacao.com.br",
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
