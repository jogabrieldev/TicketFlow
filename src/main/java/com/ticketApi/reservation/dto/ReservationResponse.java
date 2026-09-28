package com.ticketApi.reservation.dto;

import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID eventoId,
        ReservationStatus status,
        OffsetDateTime expiraEm,
        List<ReservationItemResponse> itens,
        BigDecimal valorTotal,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {

    public static ReservationResponse de(Reservation reserva) {
        List<ReservationItemResponse> itens = reserva.obterItens().stream()
                .map(ReservationItemResponse::de)
                .toList();
        BigDecimal valorTotal = itens.stream()
                .map(ReservationItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        UUID eventoId = reserva.obterItens().getFirst().obterLote().obterEvento().obterId();

        return new ReservationResponse(
                reserva.obterId(),
                eventoId,
                reserva.obterStatus(),
                reserva.obterExpiraEm(),
                itens,
                valorTotal,
                reserva.obterCriadoEm(),
                reserva.obterAtualizadoEm()
        );
    }
}
