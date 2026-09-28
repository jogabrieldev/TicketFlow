package com.ticketApi.reservation.service;

import com.ticketApi.event.entity.Event;
import com.ticketApi.event.repository.EventRepository;
import com.ticketApi.reservation.dto.CreateReservationItemRequest;
import com.ticketApi.reservation.dto.CreateReservationRequest;
import com.ticketApi.reservation.dto.ReservationResponse;
import com.ticketApi.reservation.entity.ReservationStatus;
import com.ticketApi.reservation.exception.ReservationExpirationException;
import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.ticket.repository.TicketBatchRepository;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "ticketflow.reservation.expiration.enabled=false")
@Testcontainers
class ReservationExpirationIntegrationTest {

    private static final String EMAIL = "expiracao@ticketflow.com";
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2030-01-01T12:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private ReservationService servicoDeReservas;

    @Autowired
    private ReservationExpirationService servicoDeExpiracao;

    @Autowired
    private UserRepository repositorioDeUsuarios;

    @Autowired
    private EventRepository repositorioDeEventos;

    @Autowired
    private TicketBatchRepository repositorioDeLotes;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE TABLE reservation_items, reservations, ticket_batches, events, users CASCADE");
    }

    @Test
    void deveExpirarReservaVencidaERestaurarIngressos() {
        Cenario cenario = prepararReserva(10, 3);
        definirExpiracao(cenario.reservaId(), AGORA.minusSeconds(1));

        boolean expirou = servicoDeExpiracao.expirarSeNecessario(cenario.reservaId(), AGORA);

        assertThat(expirou).isTrue();
        assertThat(status(cenario.reservaId())).isEqualTo(ReservationStatus.EXPIRADA.name());
        assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(10);
    }

    @Test
    void deveIgnorarReservaQueAindaNaoVenceu() {
        Cenario cenario = prepararReserva(10, 3);
        definirExpiracao(cenario.reservaId(), AGORA.plusMinutes(1));

        boolean expirou = servicoDeExpiracao.expirarSeNecessario(cenario.reservaId(), AGORA);

        assertThat(expirou).isFalse();
        assertThat(status(cenario.reservaId())).isEqualTo(ReservationStatus.PENDENTE.name());
        assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(7);
    }

    @Test
    void deveSerIdempotenteAoExpirarMesmaReservaNovamente() {
        Cenario cenario = prepararReserva(10, 3);
        definirExpiracao(cenario.reservaId(), AGORA.minusSeconds(1));

        boolean primeiraExecucao = servicoDeExpiracao.expirarSeNecessario(cenario.reservaId(), AGORA);
        boolean segundaExecucao = servicoDeExpiracao.expirarSeNecessario(cenario.reservaId(), AGORA);

        assertThat(primeiraExecucao).isTrue();
        assertThat(segundaExecucao).isFalse();
        assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(10);
    }

    @Test
    @Timeout(30)
    void deveRestaurarIngressosUmaUnicaVezComDuasExpiracoesConcorrentes()
            throws InterruptedException, ExecutionException {
        Cenario cenario = prepararReserva(10, 3);
        definirExpiracao(cenario.reservaId(), AGORA.minusSeconds(1));
        CountDownLatch inicio = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> primeira = executor.submit(() -> {
                inicio.await();
                return servicoDeExpiracao.expirarSeNecessario(cenario.reservaId(), AGORA);
            });
            Future<Boolean> segunda = executor.submit(() -> {
                inicio.await();
                return servicoDeExpiracao.expirarSeNecessario(cenario.reservaId(), AGORA);
            });
            inicio.countDown();

            assertThat(List.of(primeira.get(), segunda.get()))
                    .containsExactlyInAnyOrder(true, false);
        }

        assertThat(status(cenario.reservaId())).isEqualTo(ReservationStatus.EXPIRADA.name());
        assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(10);
    }

    @Test
    void deveReverterStatusQuandoRestauracaoViolarCapacidadeDoLote() {
        Cenario cenario = prepararReserva(10, 3);
        definirExpiracao(cenario.reservaId(), AGORA.minusSeconds(1));
        jdbc.update("UPDATE ticket_batches SET available_quantity = total_quantity WHERE id = ?", cenario.loteId());

        assertThatThrownBy(() -> servicoDeExpiracao.expirarSeNecessario(cenario.reservaId(), AGORA))
                .isInstanceOf(ReservationExpirationException.class);

        assertThat(status(cenario.reservaId())).isEqualTo(ReservationStatus.PENDENTE.name());
        assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(10);
    }

    @Test
    void deveLimitarQuantidadeDeCandidatasPorExecucao() {
        Cenario primeira = prepararReservaComNovoUsuario("primeira@ticketflow.com", 5, 1);
        Cenario segunda = prepararReservaComNovoUsuario("segunda@ticketflow.com", 5, 1);
        Cenario terceira = prepararReservaComNovoUsuario("terceira@ticketflow.com", 5, 1);
        definirExpiracao(primeira.reservaId(), AGORA.minusMinutes(3));
        definirExpiracao(segunda.reservaId(), AGORA.minusMinutes(2));
        definirExpiracao(terceira.reservaId(), AGORA.minusMinutes(1));

        List<UUID> candidatas = servicoDeExpiracao.buscarCandidatas(AGORA, 2);

        assertThat(candidatas).containsExactly(primeira.reservaId(), segunda.reservaId());
    }

    private Cenario prepararReserva(int quantidadeTotal, int quantidadeReservada) {
        return prepararReservaComNovoUsuario(EMAIL, quantidadeTotal, quantidadeReservada);
    }

    private Cenario prepararReservaComNovoUsuario(
            String email,
            int quantidadeTotal,
            int quantidadeReservada
    ) {
        User usuario = repositorioDeUsuarios.saveAndFlush(new User(
                "Cliente Expiração",
                email,
                "$2a$10$hashApenasParaTesteDeExpiracao",
                UserRole.CLIENTE
        ));
        Event evento = repositorioDeEventos.saveAndFlush(new Event(
                "Evento " + UUID.randomUUID(),
                null,
                "São Paulo",
                OffsetDateTime.parse("2031-10-10T09:00:00-03:00"),
                OffsetDateTime.parse("2031-10-10T18:00:00-03:00")
        ));
        TicketBatch lote = repositorioDeLotes.saveAndFlush(new TicketBatch(
                evento,
                "Lote",
                new BigDecimal("100.00"),
                quantidadeTotal
        ));
        ReservationResponse reserva = servicoDeReservas.criar(
                usuario.obterEmail(),
                new CreateReservationRequest(List.of(
                        new CreateReservationItemRequest(lote.obterId(), quantidadeReservada)
                ))
        );
        return new Cenario(reserva.id(), lote.obterId());
    }

    private void definirExpiracao(UUID reservaId, OffsetDateTime expiraEm) {
        jdbc.update("UPDATE reservations SET expires_at = ? WHERE id = ?", expiraEm, reservaId);
    }

    private int quantidadeDisponivel(UUID loteId) {
        return jdbc.queryForObject(
                "SELECT available_quantity FROM ticket_batches WHERE id = ?",
                Integer.class,
                loteId
        );
    }

    private String status(UUID reservaId) {
        return jdbc.queryForObject(
                "SELECT status FROM reservations WHERE id = ?",
                String.class,
                reservaId
        );
    }

    private record Cenario(UUID reservaId, UUID loteId) {
    }
}
