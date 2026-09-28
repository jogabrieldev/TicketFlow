package com.ticketApi.reservation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "ticketflow.reservation.expiration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ReservationExpirationScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationExpirationScheduler.class);

    private final ReservationExpirationService servicoDeExpiracao;
    private final Clock relogio;
    private final int tamanhoDoLote;

    public ReservationExpirationScheduler(
            ReservationExpirationService servicoDeExpiracao,
            Clock relogio,
            @Value("${ticketflow.reservation.expiration.batch-size:100}") int tamanhoDoLote
    ) {
        if (tamanhoDoLote <= 0) {
            throw new IllegalArgumentException("O tamanho do lote de expiração deve ser maior que zero");
        }
        this.servicoDeExpiracao = servicoDeExpiracao;
        this.relogio = relogio;
        this.tamanhoDoLote = tamanhoDoLote;
    }

    @Scheduled(fixedDelayString = "${ticketflow.reservation.expiration.scan-delay:PT30S}")
    public void expirarReservasVencidas() {
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        for (UUID reservaId : servicoDeExpiracao.buscarCandidatas(agora, tamanhoDoLote)) {
            try {
                servicoDeExpiracao.expirarSeNecessario(reservaId, agora);
            } catch (RuntimeException excecao) {
                LOG.error("Falha ao expirar a reserva {}", reservaId, excecao);
            }
        }
    }
}
