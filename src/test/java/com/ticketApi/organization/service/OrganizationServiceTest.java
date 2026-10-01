package com.ticketApi.organization.service;

import com.ticketApi.organization.dto.CreateOrganizationRequest;
import com.ticketApi.organization.dto.OrganizationPageResponse;
import com.ticketApi.organization.dto.OrganizationResponse;
import com.ticketApi.organization.entity.Organization;
import com.ticketApi.organization.entity.OrganizationMember;
import com.ticketApi.organization.entity.OrganizationMemberRole;
import com.ticketApi.organization.exception.CnpjAlreadyRegisteredException;
import com.ticketApi.organization.exception.OrganizationManagementDeniedException;
import com.ticketApi.organization.exception.OrganizationOwnerAlreadyExistsException;
import com.ticketApi.organization.repository.OrganizationMemberRepository;
import com.ticketApi.organization.repository.OrganizationRepository;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    private static final String EMAIL_USUARIO = "maria@exemplo.com";

    @Mock
    private OrganizationRepository repositorioDeOrganizacoes;

    @Mock
    private OrganizationMemberRepository repositorioDeMembros;

    @Mock
    private UserRepository repositorioDeUsuarios;

    private OrganizationService servicoDeOrganizacoes;

    @BeforeEach
    void preparar() {
        servicoDeOrganizacoes = new OrganizationService(
                repositorioDeOrganizacoes,
                repositorioDeMembros,
                repositorioDeUsuarios
        );
    }

    @Test
    void deveCriarOrganizacaoEProprietarioNaMesmaOperacao() {
        User usuario = criarUsuario(UserRole.ADMINISTRADOR);
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL_USUARIO)).willReturn(Optional.of(usuario));
        given(repositorioDeMembros.existePorUsuarioIdEPapel(
                usuario.obterId(), OrganizationMemberRole.PROPRIETARIO)).willReturn(false);
        given(repositorioDeOrganizacoes.existsByCnpj("11222333000181")).willReturn(false);
        given(repositorioDeOrganizacoes.saveAndFlush(any(Organization.class)))
                .willAnswer(invocacao -> invocacao.getArgument(0));
        given(repositorioDeMembros.saveAndFlush(any(OrganizationMember.class)))
                .willAnswer(invocacao -> invocacao.getArgument(0));

        OrganizationResponse resposta = servicoDeOrganizacoes.criar(EMAIL_USUARIO, criarRequisicao());

        ArgumentCaptor<Organization> organizacao = ArgumentCaptor.forClass(Organization.class);
        ArgumentCaptor<OrganizationMember> membro = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(repositorioDeOrganizacoes).saveAndFlush(organizacao.capture());
        verify(repositorioDeMembros).saveAndFlush(membro.capture());
        assertThat(organizacao.getValue().obterCnpj()).isEqualTo("11222333000181");
        assertThat(organizacao.getValue().obterEmail()).isEqualTo("contato@ticketflow.com.br");
        assertThat(membro.getValue().obterOrganizacao()).isSameAs(organizacao.getValue());
        assertThat(membro.getValue().obterUsuario()).isSameAs(usuario);
        assertThat(membro.getValue().obterPapel()).isEqualTo(OrganizationMemberRole.PROPRIETARIO);
        assertThat(resposta.id()).isEqualTo(organizacao.getValue().obterId());
    }

    @Test
    void deveNegarCriacaoDeOrganizacaoParaCliente() {
        User cliente = criarUsuario(UserRole.CLIENTE);
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL_USUARIO)).willReturn(Optional.of(cliente));

        assertThatThrownBy(() -> servicoDeOrganizacoes.criar(EMAIL_USUARIO, criarRequisicao()))
                .isInstanceOf(OrganizationManagementDeniedException.class);

        verify(repositorioDeOrganizacoes, never()).saveAndFlush(any());
        verify(repositorioDeMembros, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarUsuarioQueJaPossuiOrganizacao() {
        User usuario = criarUsuario(UserRole.ADMINISTRADOR);
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL_USUARIO)).willReturn(Optional.of(usuario));
        given(repositorioDeMembros.existePorUsuarioIdEPapel(
                usuario.obterId(), OrganizationMemberRole.PROPRIETARIO)).willReturn(true);

        assertThatThrownBy(() -> servicoDeOrganizacoes.criar(EMAIL_USUARIO, criarRequisicao()))
                .isInstanceOf(OrganizationOwnerAlreadyExistsException.class);

        verify(repositorioDeOrganizacoes, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarCnpjJaCadastrado() {
        User usuario = criarUsuario(UserRole.ADMINISTRADOR);
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL_USUARIO)).willReturn(Optional.of(usuario));
        given(repositorioDeMembros.existePorUsuarioIdEPapel(
                usuario.obterId(), OrganizationMemberRole.PROPRIETARIO)).willReturn(false);
        given(repositorioDeOrganizacoes.existsByCnpj("11222333000181")).willReturn(true);

        assertThatThrownBy(() -> servicoDeOrganizacoes.criar(EMAIL_USUARIO, criarRequisicao()))
                .isInstanceOf(CnpjAlreadyRegisteredException.class);
    }

    @Test
    void deveConverterConflitoConcorrenteDeCnpj() {
        User usuario = criarUsuario(UserRole.ADMINISTRADOR);
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL_USUARIO)).willReturn(Optional.of(usuario));
        given(repositorioDeMembros.existePorUsuarioIdEPapel(
                usuario.obterId(), OrganizationMemberRole.PROPRIETARIO)).willReturn(false);
        given(repositorioDeOrganizacoes.existsByCnpj("11222333000181")).willReturn(false);
        given(repositorioDeOrganizacoes.saveAndFlush(any(Organization.class)))
                .willThrow(new DataIntegrityViolationException(
                        "conflito",
                        new RuntimeException("uk_organizations_cnpj")
                ));

        assertThatThrownBy(() -> servicoDeOrganizacoes.criar(EMAIL_USUARIO, criarRequisicao()))
                .isInstanceOf(CnpjAlreadyRegisteredException.class);
    }

    @Test
    void deveNegarConsultaDeOrganizacaoParaCliente() {
        User usuario = criarUsuario(UserRole.CLIENTE);
        Organization organizacao = criarOrganizacao();
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL_USUARIO)).willReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicoDeOrganizacoes.buscarPorId(EMAIL_USUARIO, organizacao.obterId()))
                .isInstanceOf(OrganizationManagementDeniedException.class);
    }

    @Test
    void devePermitirConsultaGlobalParaAdministrador() {
        User administrador = criarUsuario(UserRole.ADMINISTRADOR);
        Organization organizacao = criarOrganizacao();
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL_USUARIO)).willReturn(Optional.of(administrador));
        given(repositorioDeOrganizacoes.findById(organizacao.obterId())).willReturn(Optional.of(organizacao));

        OrganizationResponse resposta = servicoDeOrganizacoes.buscarPorId(EMAIL_USUARIO, organizacao.obterId());

        assertThat(resposta.id()).isEqualTo(organizacao.obterId());
        verify(repositorioDeMembros, never()).existePorOrganizacaoIdEUsuarioIdEPapel(any(), any(), any());
    }

    @Test
    void deveListarTodasAsOrganizacoesParaAdministrador() {
        User administrador = criarUsuario(UserRole.ADMINISTRADOR);
        Organization organizacao = criarOrganizacao();
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL_USUARIO)).willReturn(Optional.of(administrador));
        given(repositorioDeOrganizacoes.findAll(any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new PageImpl<>(List.of(organizacao)));

        OrganizationPageResponse resposta = servicoDeOrganizacoes.listar(EMAIL_USUARIO, 0, 20);

        assertThat(resposta.conteudo()).hasSize(1);
        assertThat(resposta.conteudo().getFirst().id()).isEqualTo(organizacao.obterId());
    }

    @Test
    void deveNegarListagemDeOrganizacoesParaCliente() {
        User usuario = criarUsuario(UserRole.CLIENTE);
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL_USUARIO)).willReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicoDeOrganizacoes.listar(EMAIL_USUARIO, 0, 20))
                .isInstanceOf(OrganizationManagementDeniedException.class);
        verify(repositorioDeOrganizacoes, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    private User criarUsuario(UserRole papel) {
        return new User("Maria Silva", EMAIL_USUARIO, "hash-seguro", papel);
    }

    private CreateOrganizationRequest criarRequisicao() {
        return new CreateOrganizationRequest(
                "Ticket Flow",
                "Ticket Flow Tecnologia Ltda",
                "11.222.333/0001-81",
                "Contato@TicketFlow.com.br",
                "(11) 98765-4321",
                "Avenida Paulista",
                "1000",
                "10º andar",
                "Bela Vista",
                "São Paulo",
                "sp",
                "01310-100"
        );
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
                "10º andar",
                "Bela Vista",
                "São Paulo",
                "SP",
                "01310-100"
        );
    }
}
