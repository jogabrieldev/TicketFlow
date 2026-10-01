package com.ticketApi.reservation.service;

import com.ticketApi.event.entity.Event;
import com.ticketApi.reservation.dto.CreateReservationItemRequest;
import com.ticketApi.reservation.dto.CreateReservationRequest;
import com.ticketApi.reservation.dto.ReservationPageResponse;
import com.ticketApi.reservation.dto.ReservationResponse;
import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.exception.AuthenticatedUserNotFoundException;
import com.ticketApi.reservation.exception.DuplicateTicketBatchException;
import com.ticketApi.reservation.exception.EmptyReservationException;
import com.ticketApi.reservation.exception.MixedEventReservationException;
import com.ticketApi.reservation.repository.ReservationRepository;
import com.ticketApi.shared.idempotency.IdempotencyService;
import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.ticket.exception.InsufficientTicketAvailabilityException;
import com.ticketApi.ticket.exception.TicketBatchNotFoundException;
import com.ticketApi.ticket.repository.TicketBatchRepository;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final String EMAIL = "maria@exemplo.com";
    private static final Instant AGORA = Instant.parse("2026-09-28T14:00:00Z");

    @Mock
    private ReservationRepository repositorioDeReservas;

    @Mock
    private TicketBatchRepository repositorioDeLotes;

    @Mock
    private UserRepository repositorioDeUsuarios;

    @Mock
    private IdempotencyService servicoDeIdempotencia;

    private ReservationService servicoDeReservas;

    @BeforeEach
    void preparar() {
        Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
        servicoDeReservas = new ReservationService(
                repositorioDeReservas,
                repositorioDeLotes,
                repositorioDeUsuarios,
                Duration.ofMinutes(15),
                relogio,
                servicoDeIdempotencia
        );
    }

    @Test
    void deveCriarReservaParaUsuarioAutenticado() {
        User usuario = criarUsuario();
        Event evento = criarEvento("Java Conference");
        TicketBatch primeiroLote = criarLote(evento, "Primeiro lote", "100.00", 10);
        TicketBatch segundoLote = criarLote(evento, "Segundo lote", "150.00", 5);
        CreateReservationRequest requisicao = requisicao(
                new CreateReservationItemRequest(primeiroLote.obterId(), 2),
                new CreateReservationItemRequest(segundoLote.obterId(), 1)
        );
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repositorioDeLotes.findAllById(any())).willReturn(List.of(primeiroLote, segundoLote));
        given(repositorioDeLotes.reservarSeDisponivel(
                any(UUID.class),
                anyInt(),
                any(OffsetDateTime.class)
        )).willReturn(1);
        given(repositorioDeReservas.saveAndFlush(any(Reservation.class)))
                .willAnswer(invocacao -> invocacao.getArgument(0));

        ReservationResponse resposta = servicoDeReservas.criar(EMAIL, requisicao);

        ArgumentCaptor<Reservation> capturador = ArgumentCaptor.forClass(Reservation.class);
        verify(repositorioDeReservas).saveAndFlush(capturador.capture());
        Reservation reserva = capturador.getValue();
        assertThat(reserva.obterUsuario()).isSameAs(usuario);
        assertThat(reserva.obterExpiraEm()).isEqualTo(OffsetDateTime.parse("2026-09-28T14:15:00Z"));
        assertThat(reserva.obterItens()).hasSize(2);
        verify(repositorioDeLotes).reservarSeDisponivel(
                primeiroLote.obterId(),
                2,
                OffsetDateTime.parse("2026-09-28T14:00:00Z")
        );
        verify(repositorioDeLotes).reservarSeDisponivel(
                segundoLote.obterId(),
                1,
                OffsetDateTime.parse("2026-09-28T14:00:00Z")
        );
        assertThat(resposta.valorTotal()).isEqualByComparingTo("350.00");
        assertThat(resposta.eventoId()).isEqualTo(evento.obterId());
    }

    @Test
    void deveBaixarLotesEmOrdemDeterministicaDeIdentificador() {
        User usuario = criarUsuario();
        Event evento = criarEvento("Java Conference");
        TicketBatch loteUm = criarLote(evento, "Lote um", "100.00", 10);
        TicketBatch loteDois = criarLote(evento, "Lote dois", "100.00", 10);
        List<TicketBatch> lotesOrdenados = List.of(loteUm, loteDois).stream()
                .sorted(java.util.Comparator.comparing(TicketBatch::obterId))
                .toList();
        TicketBatch primeiro = lotesOrdenados.get(0);
        TicketBatch segundo = lotesOrdenados.get(1);
        CreateReservationRequest requisicao = requisicao(
                new CreateReservationItemRequest(segundo.obterId(), 1),
                new CreateReservationItemRequest(primeiro.obterId(), 1)
        );
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repositorioDeLotes.findAllById(any())).willReturn(List.of(loteUm, loteDois));
        given(repositorioDeLotes.reservarSeDisponivel(
                any(UUID.class),
                anyInt(),
                any(OffsetDateTime.class)
        )).willReturn(1);
        given(repositorioDeReservas.saveAndFlush(any(Reservation.class)))
                .willAnswer(invocacao -> invocacao.getArgument(0));

        servicoDeReservas.criar(EMAIL, requisicao);

        InOrder ordem = inOrder(repositorioDeLotes);
        ordem.verify(repositorioDeLotes).reservarSeDisponivel(
                eq(primeiro.obterId()),
                eq(1),
                any(OffsetDateTime.class)
        );
        ordem.verify(repositorioDeLotes).reservarSeDisponivel(
                eq(segundo.obterId()),
                eq(1),
                any(OffsetDateTime.class)
        );
    }

    @Test
    void deveRejeitarUsuarioAutenticadoAusente() {
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicoDeReservas.criar(
                EMAIL,
                requisicao(new CreateReservationItemRequest(UUID.randomUUID(), 1))))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);
    }

    @Test
    void deveRejeitarLoteDuplicado() {
        User usuario = criarUsuario();
        UUID loteId = UUID.randomUUID();
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicoDeReservas.criar(
                EMAIL,
                requisicao(
                        new CreateReservationItemRequest(loteId, 1),
                        new CreateReservationItemRequest(loteId, 2))))
                .isInstanceOf(DuplicateTicketBatchException.class);
        verify(repositorioDeLotes, never()).findAllById(any());
    }

    @Test
    void deveRejeitarReservaSemItensNaCamadaDeServico() {
        User usuario = criarUsuario();
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicoDeReservas.criar(EMAIL, new CreateReservationRequest(List.of())))
                .isInstanceOf(EmptyReservationException.class);
        verify(repositorioDeLotes, never()).findAllById(any());
    }

    @Test
    void deveRejeitarLoteInexistente() {
        User usuario = criarUsuario();
        UUID loteId = UUID.randomUUID();
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repositorioDeLotes.findAllById(any())).willReturn(List.of());

        assertThatThrownBy(() -> servicoDeReservas.criar(
                EMAIL,
                requisicao(new CreateReservationItemRequest(loteId, 1))))
                .isInstanceOf(TicketBatchNotFoundException.class);
    }

    @Test
    void deveRejeitarLotesDeEventosDiferentes() {
        User usuario = criarUsuario();
        TicketBatch loteUm = criarLote(criarEvento("Evento um"), "Lote um", "100.00", 10);
        TicketBatch loteDois = criarLote(criarEvento("Evento dois"), "Lote dois", "100.00", 10);
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repositorioDeLotes.findAllById(any())).willReturn(List.of(loteUm, loteDois));

        assertThatThrownBy(() -> servicoDeReservas.criar(
                EMAIL,
                requisicao(
                        new CreateReservationItemRequest(loteUm.obterId(), 1),
                        new CreateReservationItemRequest(loteDois.obterId(), 1))))
                .isInstanceOf(MixedEventReservationException.class);
        verify(repositorioDeReservas, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarQuantidadeIndisponivel() {
        User usuario = criarUsuario();
        TicketBatch lote = criarLote(criarEvento("Java Conference"), "Lote", "100.00", 2);
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repositorioDeLotes.findAllById(any())).willReturn(List.of(lote));

        assertThatThrownBy(() -> servicoDeReservas.criar(
                EMAIL,
                requisicao(new CreateReservationItemRequest(lote.obterId(), 3))))
                .isInstanceOf(InsufficientTicketAvailabilityException.class);
        verify(repositorioDeReservas, never()).saveAndFlush(any());
    }

    @Test
    void deveListarReservasDoUsuarioAutenticado() {
        User usuario = criarUsuario();
        TicketBatch lote = criarLote(criarEvento("Java Conference"), "Lote", "100.00", 10);
        Reservation reserva = new Reservation(
                usuario,
                OffsetDateTime.ofInstant(AGORA.plusSeconds(900), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC)
        );
        reserva.adicionarItem(lote, 1);
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repositorioDeReservas.buscarPorUsuarioId(eq(usuario.obterId()), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(reserva)));

        ReservationPageResponse resposta = servicoDeReservas.listarDoUsuario(EMAIL, 0, 20);

        assertThat(resposta.conteudo()).hasSize(1);
        assertThat(resposta.conteudo().getFirst().id()).isEqualTo(reserva.obterId());
        ArgumentCaptor<Pageable> capturador = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorioDeReservas).buscarPorUsuarioId(eq(usuario.obterId()), capturador.capture());
        assertThat(capturador.getValue().getSort().getOrderFor("criadoEm").isDescending()).isTrue();
    }

    private static CreateReservationRequest requisicao(CreateReservationItemRequest... itens) {
        return new CreateReservationRequest(List.of(itens));
    }

    private static User criarUsuario() {
        return new User("Maria Silva", EMAIL, "hash-da-senha", UserRole.CLIENTE);
    }

    private static Event criarEvento(String nome) {
        return new Event(
                com.ticketApi.organization.OrganizationTestFactory.criarOrganizacao(),
                nome,
                null,
                "Centro de Convenções",
                OffsetDateTime.parse("2026-10-10T09:00:00-03:00"),
                OffsetDateTime.parse("2026-10-10T18:00:00-03:00")
        );
    }

    private static TicketBatch criarLote(
            Event evento,
            String nome,
            String preco,
            int quantidade
    ) {
        return new TicketBatch(evento, nome, new BigDecimal(preco), quantidade);
    }
}
