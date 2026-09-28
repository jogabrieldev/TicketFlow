package com.ticketApi.reservation.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReservationExpirationSchedulerTest {

    @Test
    void deveDelegarCandidatasAoServicoComMesmoInstante() {
        ReservationExpirationService servico = mock(ReservationExpirationService.class);
        Clock relogio = Clock.fixed(Instant.parse("2026-09-28T15:00:00Z"), ZoneOffset.UTC);
        ReservationExpirationScheduler agendador = new ReservationExpirationScheduler(servico, relogio, 50);
        OffsetDateTime agora = OffsetDateTime.parse("2026-09-28T15:00:00Z");
        UUID reservaUm = UUID.randomUUID();
        UUID reservaDois = UUID.randomUUID();
        given(servico.buscarCandidatas(agora, 50)).willReturn(List.of(reservaUm, reservaDois));

        agendador.expirarReservasVencidas();

        verify(servico).expirarSeNecessario(reservaUm, agora);
        verify(servico).expirarSeNecessario(reservaDois, agora);
    }

    @Test
    void deveContinuarProcessamentoQuandoUmaReservaFalhar() {
        ReservationExpirationService servico = mock(ReservationExpirationService.class);
        Clock relogio = Clock.fixed(Instant.parse("2026-09-28T15:00:00Z"), ZoneOffset.UTC);
        ReservationExpirationScheduler agendador = new ReservationExpirationScheduler(servico, relogio, 50);
        OffsetDateTime agora = OffsetDateTime.parse("2026-09-28T15:00:00Z");
        UUID reservaComFalha = UUID.randomUUID();
        UUID proximaReserva = UUID.randomUUID();
        given(servico.buscarCandidatas(agora, 50)).willReturn(List.of(reservaComFalha, proximaReserva));
        doThrow(new IllegalStateException("falha simulada"))
                .when(servico).expirarSeNecessario(reservaComFalha, agora);

        agendador.expirarReservasVencidas();

        verify(servico).expirarSeNecessario(proximaReserva, agora);
    }
}
