package com.ticketApi.ticket.service;

import com.ticketApi.event.entity.Event;
import com.ticketApi.event.exception.EventNotFoundException;
import com.ticketApi.event.repository.EventRepository;
import com.ticketApi.organization.exception.OrganizationAccessDeniedException;
import com.ticketApi.ticket.dto.CreateTicketBatchRequest;
import com.ticketApi.ticket.dto.TicketBatchPageResponse;
import com.ticketApi.ticket.dto.TicketBatchResponse;
import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.ticket.exception.TicketBatchNotFoundException;
import com.ticketApi.ticket.repository.TicketBatchRepository;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketBatchServiceTest {

    @Mock
    private TicketBatchRepository repositorioDeLotes;

    @Mock
    private EventRepository repositorioDeEventos;

    @Mock
    private UserRepository repositorioDeUsuarios;

    private TicketBatchService servicoDeLotes;

    @BeforeEach
    void preparar() {
        servicoDeLotes = new TicketBatchService(
                repositorioDeLotes,
                repositorioDeEventos,
                repositorioDeUsuarios
        );
    }

    @Test
    void deveCriarLoteVinculadoAoEvento() {
        User usuario = criarUsuario(UserRole.ADMINISTRADOR);
        Event evento = criarEvento();
        CreateTicketBatchRequest requisicao = new CreateTicketBatchRequest(
                "Primeiro lote",
                new BigDecimal("100.00"),
                500
        );
        given(repositorioDeUsuarios.buscarPorEmail(usuario.obterEmail())).willReturn(Optional.of(usuario));
        given(repositorioDeEventos.findById(evento.obterId())).willReturn(Optional.of(evento));
        given(repositorioDeLotes.save(any(TicketBatch.class))).willAnswer(invocacao -> invocacao.getArgument(0));

        TicketBatchResponse resposta = servicoDeLotes.criar(usuario.obterEmail(), evento.obterId(), requisicao);

        ArgumentCaptor<TicketBatch> capturador = ArgumentCaptor.forClass(TicketBatch.class);
        verify(repositorioDeLotes).save(capturador.capture());
        assertThat(capturador.getValue().obterEvento()).isSameAs(evento);
        assertThat(resposta.eventoId()).isEqualTo(evento.obterId());
        assertThat(resposta.quantidadeDisponivel()).isEqualTo(500);
    }

    @Test
    void deveRejeitarCriacaoQuandoEventoNaoExistir() {
        User usuario = criarUsuario(UserRole.CLIENTE);
        UUID eventoId = UUID.randomUUID();
        CreateTicketBatchRequest requisicao = new CreateTicketBatchRequest(
                "Primeiro lote",
                new BigDecimal("100.00"),
                500
        );
        given(repositorioDeUsuarios.buscarPorEmail(usuario.obterEmail())).willReturn(Optional.of(usuario));
        given(repositorioDeEventos.findById(eventoId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicoDeLotes.criar(usuario.obterEmail(), eventoId, requisicao))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessage("Evento não encontrado: " + eventoId);
    }

    @Test
    void deveNegarCriacaoParaClienteQueNaoEOProprietario() {
        User usuario = criarUsuario(UserRole.CLIENTE);
        Event evento = criarEvento();
        CreateTicketBatchRequest requisicao = new CreateTicketBatchRequest(
                "Primeiro lote", new BigDecimal("100.00"), 500);
        given(repositorioDeUsuarios.buscarPorEmail(usuario.obterEmail())).willReturn(Optional.of(usuario));
        given(repositorioDeEventos.findById(evento.obterId())).willReturn(Optional.of(evento));
        assertThatThrownBy(() -> servicoDeLotes.criar(usuario.obterEmail(), evento.obterId(), requisicao))
                .isInstanceOf(OrganizationAccessDeniedException.class);
    }

    @Test
    void devePermitirCriacaoParaAdministradorGlobal() {
        User administrador = criarUsuario(UserRole.ADMINISTRADOR);
        Event evento = criarEvento();
        CreateTicketBatchRequest requisicao = new CreateTicketBatchRequest(
                "Primeiro lote", new BigDecimal("100.00"), 500);
        given(repositorioDeUsuarios.buscarPorEmail(administrador.obterEmail()))
                .willReturn(Optional.of(administrador));
        given(repositorioDeEventos.findById(evento.obterId())).willReturn(Optional.of(evento));
        given(repositorioDeLotes.save(any(TicketBatch.class))).willAnswer(invocacao -> invocacao.getArgument(0));

        TicketBatchResponse resposta = servicoDeLotes.criar(
                administrador.obterEmail(), evento.obterId(), requisicao);

        assertThat(resposta.eventoId()).isEqualTo(evento.obterId());
    }

    @Test
    void deveBuscarLotePorId() {
        TicketBatch lote = criarLote();
        given(repositorioDeLotes.findById(lote.obterId())).willReturn(Optional.of(lote));

        TicketBatchResponse resposta = servicoDeLotes.buscarPorId(lote.obterId());

        assertThat(resposta.id()).isEqualTo(lote.obterId());
        assertThat(resposta.nome()).isEqualTo("Primeiro lote");
    }

    @Test
    void deveLancarExcecaoQuandoLoteNaoExistir() {
        UUID loteId = UUID.randomUUID();
        given(repositorioDeLotes.findById(loteId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicoDeLotes.buscarPorId(loteId))
                .isInstanceOf(TicketBatchNotFoundException.class)
                .hasMessage("Lote de ingressos não encontrado: " + loteId);
    }

    @Test
    void deveListarLotesDoEventoComPaginacao() {
        TicketBatch lote = criarLote();
        UUID eventoId = lote.obterEvento().obterId();
        given(repositorioDeEventos.existsById(eventoId)).willReturn(true);
        given(repositorioDeLotes.buscarPorEventoId(eq(eventoId), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(lote)));

        TicketBatchPageResponse resposta = servicoDeLotes.listarPorEvento(eventoId, 0, 20);

        ArgumentCaptor<Pageable> capturador = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorioDeLotes).buscarPorEventoId(eq(eventoId), capturador.capture());
        assertThat(capturador.getValue().getSort().getOrderFor("nome")).isNotNull();
        assertThat(resposta.conteudo()).hasSize(1);
        assertThat(resposta.conteudo().getFirst().id()).isEqualTo(lote.obterId());
    }

    @Test
    void deveRejeitarListagemQuandoEventoNaoExistir() {
        UUID eventoId = UUID.randomUUID();
        given(repositorioDeEventos.existsById(eventoId)).willReturn(false);

        assertThatThrownBy(() -> servicoDeLotes.listarPorEvento(eventoId, 0, 20))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessage("Evento não encontrado: " + eventoId);
    }

    private TicketBatch criarLote() {
        return new TicketBatch(criarEvento(), "Primeiro lote", new BigDecimal("100.00"), 500);
    }

    private Event criarEvento() {
        OffsetDateTime inicioEm = OffsetDateTime.parse("2026-10-10T09:00:00-03:00");
        OffsetDateTime terminoEm = OffsetDateTime.parse("2026-10-10T18:00:00-03:00");
        return new Event(
                com.ticketApi.organization.OrganizationTestFactory.criarOrganizacao(),
                "Java Conference", null, "Centro de Convenções", inicioEm, terminoEm);
    }

    private User criarUsuario(UserRole papel) {
        return new User("Maria", "maria@exemplo.com", "hash", papel);
    }
}
