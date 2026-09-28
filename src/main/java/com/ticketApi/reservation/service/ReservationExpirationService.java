package com.ticketApi.reservation.service;

import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.entity.ReservationItem;
import com.ticketApi.reservation.entity.ReservationStatus;
import com.ticketApi.reservation.event.ReservationExpiredEvent;
import com.ticketApi.reservation.exception.ReservationExpirationException;
import com.ticketApi.reservation.repository.ReservationRepository;
import com.ticketApi.ticket.repository.TicketBatchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationExpirationService {

    private final ReservationRepository repositorioDeReservas;
    private final TicketBatchRepository repositorioDeLotes;
    private final ApplicationEventPublisher publicadorDeEventos;

    public ReservationExpirationService(
            ReservationRepository repositorioDeReservas,
            TicketBatchRepository repositorioDeLotes,
            ApplicationEventPublisher publicadorDeEventos
    ) {
        this.repositorioDeReservas = repositorioDeReservas;
        this.repositorioDeLotes = repositorioDeLotes;
        this.publicadorDeEventos = publicadorDeEventos;
    }

    @Transactional(readOnly = true)
    public List<UUID> buscarCandidatas(OffsetDateTime agora, int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("O limite de reservas deve ser maior que zero");
        }
        return repositorioDeReservas.buscarIdsParaExpiracao(
                ReservationStatus.PENDENTE,
                agora,
                PageRequest.of(0, limite)
        );
    }

    @Transactional
    public boolean expirarSeNecessario(UUID reservaId, OffsetDateTime agora) {
        int reservasAtualizadas = repositorioDeReservas.marcarComoExpiradaSePendente(
                reservaId,
                ReservationStatus.PENDENTE,
                ReservationStatus.EXPIRADA,
                agora
        );
        if (reservasAtualizadas == 0) {
            return false;
        }

        Reservation reserva = repositorioDeReservas.buscarComItensPorId(reservaId)
                .orElseThrow(() -> new ReservationExpirationException(reservaId, "reserva não encontrada"));
        if (reserva.obterItens().isEmpty()) {
            throw new ReservationExpirationException(reservaId, "reserva sem itens");
        }

        for (ReservationItem item : reserva.obterItens()) {
            int lotesAtualizados = repositorioDeLotes.restaurarDisponibilidade(
                    item.obterLote().obterId(),
                    item.obterQuantidade(),
                    agora
            );
            if (lotesAtualizados == 0) {
                throw new ReservationExpirationException(
                        reservaId,
                        "não foi possível restaurar o lote " + item.obterLote().obterId()
                );
            }
        }
        publicadorDeEventos.publishEvent(new ReservationExpiredEvent(reservaId, agora));
        return true;
    }
}
