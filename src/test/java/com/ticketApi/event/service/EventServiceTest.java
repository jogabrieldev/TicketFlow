package com.ticketApi.event.service;

import com.ticketApi.event.dto.CreateEventRequest;
import com.ticketApi.event.dto.EventPageResponse;
import com.ticketApi.event.dto.EventResponse;
import com.ticketApi.event.entity.Event;
import com.ticketApi.event.exception.EventNotFoundException;
import com.ticketApi.event.repository.EventRepository;
import com.ticketApi.organization.exception.OrganizationAccessDeniedException;
import com.ticketApi.organization.exception.OrganizationNotFoundException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static com.ticketApi.organization.OrganizationTestFactory.criarOrganizacao;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final OffsetDateTime INICIO_EM =
            OffsetDateTime.parse("2026-10-10T09:00:00-03:00");
    private static final OffsetDateTime TERMINO_EM =
            OffsetDateTime.parse("2026-10-10T18:00:00-03:00");

    @Mock
    private EventRepository repositorioDeEventos;

    @Mock
    private OrganizationRepository repositorioDeOrganizacoes;

    @Mock
    private UserRepository repositorioDeUsuarios;

    private EventService servicoDeEventos;

    @BeforeEach
    void preparar() {
        servicoDeEventos = new EventService(
                repositorioDeEventos,
                repositorioDeOrganizacoes,
                repositorioDeUsuarios
        );
    }

    @Test
    void deveCriarEvento() {
        User usuario = new User("Admin", "admin@exemplo.com", "hash", UserRole.ADMINISTRADOR);
        var organizacao = criarOrganizacao();
        CreateEventRequest requisicao = new CreateEventRequest(
                "Java Conference",
                "Java ecosystem",
                "Convention Center",
                INICIO_EM,
                TERMINO_EM
        );
        given(repositorioDeUsuarios.buscarPorEmail(usuario.obterEmail())).willReturn(Optional.of(usuario));
        given(repositorioDeOrganizacoes.findById(organizacao.obterId())).willReturn(Optional.of(organizacao));
        given(repositorioDeEventos.save(any(Event.class))).willAnswer(invocacao -> invocacao.getArgument(0));

        EventResponse resposta = servicoDeEventos.criar(usuario.obterEmail(), organizacao.obterId(), requisicao);

        ArgumentCaptor<Event> capturador = ArgumentCaptor.forClass(Event.class);
        verify(repositorioDeEventos).save(capturador.capture());
        assertThat(capturador.getValue().obterNome()).isEqualTo("Java Conference");
        assertThat(capturador.getValue().obterOrganizacao()).isSameAs(organizacao);
        assertThat(resposta.id()).isNotNull();
        assertThat(resposta.organizacaoId()).isEqualTo(organizacao.obterId());
        assertThat(resposta.local()).isEqualTo("Convention Center");
    }

    @Test
    void deveNegarCriacaoParaClienteQueNaoEOProprietario() {
        User usuario = new User("Maria", "maria@exemplo.com", "hash", UserRole.CLIENTE);
        var organizacao = criarOrganizacao();
        CreateEventRequest requisicao = new CreateEventRequest(
                "Java Conference", "Java ecosystem", "Convention Center", INICIO_EM, TERMINO_EM);
        given(repositorioDeUsuarios.buscarPorEmail(usuario.obterEmail())).willReturn(Optional.of(usuario));
        given(repositorioDeOrganizacoes.findById(organizacao.obterId())).willReturn(Optional.of(organizacao));
        assertThatThrownBy(() -> servicoDeEventos.criar(usuario.obterEmail(), organizacao.obterId(), requisicao))
                .isInstanceOf(OrganizationAccessDeniedException.class);
    }

    @Test
    void deveRejeitarCriacaoParaOrganizacaoInexistente() {
        User usuario = new User("Maria", "maria@exemplo.com", "hash", UserRole.CLIENTE);
        UUID organizacaoId = UUID.randomUUID();
        CreateEventRequest requisicao = new CreateEventRequest(
                "Java Conference", "Java ecosystem", "Convention Center", INICIO_EM, TERMINO_EM);
        given(repositorioDeUsuarios.buscarPorEmail(usuario.obterEmail())).willReturn(Optional.of(usuario));
        given(repositorioDeOrganizacoes.findById(organizacaoId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicoDeEventos.criar(usuario.obterEmail(), organizacaoId, requisicao))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    void devePermitirCriacaoParaAdministradorGlobal() {
        User administrador = new User("Admin", "admin@exemplo.com", "hash", UserRole.ADMINISTRADOR);
        var organizacao = criarOrganizacao();
        CreateEventRequest requisicao = new CreateEventRequest(
                "Java Conference", "Java ecosystem", "Convention Center", INICIO_EM, TERMINO_EM);
        given(repositorioDeUsuarios.buscarPorEmail(administrador.obterEmail())).willReturn(Optional.of(administrador));
        given(repositorioDeOrganizacoes.findById(organizacao.obterId())).willReturn(Optional.of(organizacao));
        given(repositorioDeEventos.save(any(Event.class))).willAnswer(invocacao -> invocacao.getArgument(0));

        EventResponse resposta = servicoDeEventos.criar(
                administrador.obterEmail(), organizacao.obterId(), requisicao);

        assertThat(resposta.organizacaoId()).isEqualTo(organizacao.obterId());
    }

    @Test
    void deveBuscarEventoPorId() {
        Event evento = criarEvento();
        given(repositorioDeEventos.findById(evento.obterId())).willReturn(Optional.of(evento));

        EventResponse resposta = servicoDeEventos.buscarPorId(evento.obterId());

        assertThat(resposta.id()).isEqualTo(evento.obterId());
        assertThat(resposta.nome()).isEqualTo("Java Conference");
    }

    @Test
    void deveLancarExcecaoQuandoEventoNaoExistir() {
        UUID eventoId = UUID.randomUUID();
        given(repositorioDeEventos.findById(eventoId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicoDeEventos.buscarPorId(eventoId))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessage("Evento não encontrado: " + eventoId);
    }

    @Test
    void deveListarEventosComPaginacaoSolicitadaEOrdenacaoPorInicio() {
        Event evento = criarEvento();
        given(repositorioDeEventos.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(evento)));

        EventPageResponse resposta = servicoDeEventos.listar(0, 20);

        ArgumentCaptor<Pageable> capturador = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorioDeEventos).findAll(capturador.capture());
        assertThat(capturador.getValue().getPageNumber()).isZero();
        assertThat(capturador.getValue().getPageSize()).isEqualTo(20);
        assertThat(capturador.getValue().getSort().getOrderFor("inicioEm")).isNotNull();
        assertThat(resposta.conteudo()).hasSize(1);
        assertThat(resposta.conteudo().getFirst().id()).isEqualTo(evento.obterId());
    }

    private Event criarEvento() {
        return new Event(
                criarOrganizacao(),
                "Java Conference",
                "Java ecosystem",
                "Convention Center",
                INICIO_EM,
                TERMINO_EM
        );
    }
}
