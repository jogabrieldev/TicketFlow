package com.ticketApi.reservation.service;

import com.ticketApi.event.entity.Event;
import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.entity.ReservationStatus;
import com.ticketApi.reservation.event.ReservationExpiredEvent;
import com.ticketApi.reservation.exception.ReservationExpirationException;
import com.ticketApi.reservation.repository.ReservationRepository;
import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.ticket.repository.TicketBatchRepository;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationServiceTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-09-28T15:00:00Z");

    @Mock
    private ReservationRepository repositorioDeReservas;

    @Mock
    private TicketBatchRepository repositorioDeLotes;

    @Mock
    private ApplicationEventPublisher publicadorDeEventos;

    private ReservationExpirationService servicoDeExpiracao;

    @BeforeEach
    void preparar() {
        servicoDeExpiracao = new ReservationExpirationService(
                repositorioDeReservas,
                repositorioDeLotes,
                publicadorDeEventos
        );
    }

    @Test
    void deveBuscarCandidatasRespeitandoLimite() {
        UUID reservaId = UUID.randomUUID();
        given(repositorioDeReservas.buscarIdsParaExpiracao(
                org.mockito.ArgumentMatchers.eq(ReservationStatus.PENDENTE),
                org.mockito.ArgumentMatchers.eq(AGORA),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).willReturn(List.of(reservaId));

        List<UUID> resultado = servicoDeExpiracao.buscarCandidatas(AGORA, 25);

        assertThat(resultado).containsExactly(reservaId);
        ArgumentCaptor<Pageable> capturador = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorioDeReservas).buscarIdsParaExpiracao(
                org.mockito.ArgumentMatchers.eq(ReservationStatus.PENDENTE),
                org.mockito.ArgumentMatchers.eq(AGORA),
                capturador.capture()
        );
        assertThat(capturador.getValue().getPageSize()).isEqualTo(25);
    }

    @Test
    void deveRejeitarLimiteInvalido() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> servicoDeExpiracao.buscarCandidatas(AGORA, 0))
                .withMessage("O limite de reservas deve ser maior que zero");
    }

    @Test
    void deveIgnorarReservaQueNaoPuderSerReivindicada() {
        UUID reservaId = UUID.randomUUID();
        given(repositorioDeReservas.marcarComoExpiradaSePendente(
                reservaId,
                ReservationStatus.PENDENTE,
                ReservationStatus.EXPIRADA,
                AGORA
        )).willReturn(0);

        boolean expirou = servicoDeExpiracao.expirarSeNecessario(reservaId, AGORA);

        assertThat(expirou).isFalse();
        verify(repositorioDeReservas, never()).buscarComItensPorId(reservaId);
    }

    @Test
    void deveExpirarERestaurarTodosOsItens() {
        Reservation reserva = criarReservaComDoisItens();
        given(repositorioDeReservas.marcarComoExpiradaSePendente(
                reserva.obterId(),
                ReservationStatus.PENDENTE,
                ReservationStatus.EXPIRADA,
                AGORA
        )).willReturn(1);
        given(repositorioDeReservas.buscarComItensPorId(reserva.obterId())).willReturn(Optional.of(reserva));
        for (var item : reserva.obterItens()) {
            given(repositorioDeLotes.restaurarDisponibilidade(
                    item.obterLote().obterId(),
                    item.obterQuantidade(),
                    AGORA
            )).willReturn(1);
        }

        boolean expirou = servicoDeExpiracao.expirarSeNecessario(reserva.obterId(), AGORA);

        assertThat(expirou).isTrue();
        for (var item : reserva.obterItens()) {
            verify(repositorioDeLotes).restaurarDisponibilidade(
                    item.obterLote().obterId(),
                    item.obterQuantidade(),
                    AGORA
            );
        }
        ArgumentCaptor<ReservationExpiredEvent> evento = ArgumentCaptor.forClass(ReservationExpiredEvent.class);
        verify(publicadorDeEventos).publishEvent(evento.capture());
        assertThat(evento.getValue().reservaId()).isEqualTo(reserva.obterId());
        assertThat(evento.getValue().ocorridoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveFalharQuandoNaoForPossivelRestaurarUmLote() {
        Reservation reserva = criarReservaComDoisItens();
        var primeiroItem = reserva.obterItens().getFirst();
        given(repositorioDeReservas.marcarComoExpiradaSePendente(
                reserva.obterId(),
                ReservationStatus.PENDENTE,
                ReservationStatus.EXPIRADA,
                AGORA
        )).willReturn(1);
        given(repositorioDeReservas.buscarComItensPorId(reserva.obterId())).willReturn(Optional.of(reserva));
        given(repositorioDeLotes.restaurarDisponibilidade(
                primeiroItem.obterLote().obterId(),
                primeiroItem.obterQuantidade(),
                AGORA
        )).willReturn(0);

        assertThatThrownBy(() -> servicoDeExpiracao.expirarSeNecessario(reserva.obterId(), AGORA))
                .isInstanceOf(ReservationExpirationException.class)
                .hasMessageContaining("não foi possível restaurar o lote");
        verify(publicadorDeEventos, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private static Reservation criarReservaComDoisItens() {
        User usuario = new User("Maria", "maria@exemplo.com", "hash", UserRole.CLIENTE);
        Event evento = new Event(
                "Evento",
                null,
                "São Paulo",
                OffsetDateTime.parse("2026-10-10T09:00:00-03:00"),
                OffsetDateTime.parse("2026-10-10T18:00:00-03:00")
        );
        TicketBatch loteUm = new TicketBatch(evento, "Lote um", new BigDecimal("100.00"), 10);
        TicketBatch loteDois = new TicketBatch(evento, "Lote dois", new BigDecimal("150.00"), 10);
        Reservation reserva = new Reservation(usuario, AGORA.minusMinutes(1), AGORA.minusMinutes(16));
        reserva.adicionarItem(loteUm, 2);
        reserva.adicionarItem(loteDois, 1);
        return reserva;
    }
}
