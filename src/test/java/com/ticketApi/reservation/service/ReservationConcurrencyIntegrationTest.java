package com.ticketApi.reservation.service;

import com.ticketApi.event.entity.Event;
import com.ticketApi.event.repository.EventRepository;
import com.ticketApi.reservation.dto.CreateReservationItemRequest;
import com.ticketApi.reservation.dto.CreateReservationRequest;
import com.ticketApi.reservation.repository.ReservationRepository;
import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.ticket.exception.InsufficientTicketAvailabilityException;
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
import java.util.ArrayList;
import java.util.Comparator;
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
class ReservationConcurrencyIntegrationTest {

    private static final String EMAIL = "concorrencia@ticketflow.com";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private ReservationService servicoDeReservas;

    @Autowired
    private UserRepository repositorioDeUsuarios;

    @Autowired
    private EventRepository repositorioDeEventos;

    @Autowired
    private TicketBatchRepository repositorioDeLotes;

    @Autowired
    private ReservationRepository repositorioDeReservas;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE TABLE reservation_items, reservations, ticket_batches, events, users CASCADE");
    }

    @Test
    @Timeout(30)
    void devePermitirSomenteUmaReservaQuandoDoisClientesDisputamOUltimoIngresso() throws Exception {
        TicketBatch lote = prepararLote(1);

        ResultadoConcorrente resultado = executarReservasConcorrentes(lote.obterId(), 2);

        assertThat(resultado.sucessos()).isEqualTo(1);
        assertThat(resultado.indisponiveis()).isEqualTo(1);
        assertThat(quantidadeDisponivel(lote.obterId())).isZero();
        assertThat(repositorioDeReservas.count()).isEqualTo(1);
    }

    @Test
    @Timeout(120)
    void deveImpedirOversellingComMilTentativasParaCemIngressos() throws Exception {
        TicketBatch lote = prepararLote(100);

        ResultadoConcorrente resultado = executarReservasConcorrentes(lote.obterId(), 1_000);

        assertThat(resultado.sucessos()).isEqualTo(100);
        assertThat(resultado.indisponiveis()).isEqualTo(900);
        assertThat(quantidadeDisponivel(lote.obterId())).isZero();
        assertThat(repositorioDeReservas.count()).isEqualTo(100);
    }

    @Test
    void deveReverterBaixaAnteriorQuandoOutroLoteDaReservaEstiverIndisponivel() {
        User usuario = prepararUsuario();
        Event evento = prepararEvento();
        TicketBatch loteUm = repositorioDeLotes.saveAndFlush(criarLote(evento, "Lote um", 5));
        TicketBatch loteDois = repositorioDeLotes.saveAndFlush(criarLote(evento, "Lote dois", 5));
        List<TicketBatch> lotesOrdenados = List.of(loteUm, loteDois).stream()
                .sorted(Comparator.comparing(TicketBatch::obterId))
                .toList();
        TicketBatch primeiroLote = lotesOrdenados.get(0);
        TicketBatch segundoLote = lotesOrdenados.get(1);
        jdbc.update(
                "UPDATE ticket_batches SET available_quantity = 0 WHERE id = ?",
                segundoLote.obterId()
        );
        CreateReservationRequest requisicao = new CreateReservationRequest(List.of(
                new CreateReservationItemRequest(primeiroLote.obterId(), 1),
                new CreateReservationItemRequest(segundoLote.obterId(), 1)
        ));

        assertThatThrownBy(() -> servicoDeReservas.criar(usuario.obterEmail(), requisicao))
                .isInstanceOf(InsufficientTicketAvailabilityException.class);

        assertThat(quantidadeDisponivel(primeiroLote.obterId())).isEqualTo(5);
        assertThat(quantidadeDisponivel(segundoLote.obterId())).isZero();
        assertThat(repositorioDeReservas.count()).isZero();
    }

    private ResultadoConcorrente executarReservasConcorrentes(UUID loteId, int tentativas)
            throws InterruptedException, ExecutionException {
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>(tentativas);
        CreateReservationRequest requisicao = new CreateReservationRequest(
                List.of(new CreateReservationItemRequest(loteId, 1))
        );

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tentativas; i++) {
                resultados.add(executor.submit(() -> {
                    inicio.await();
                    try {
                        servicoDeReservas.criar(EMAIL, requisicao);
                        return true;
                    } catch (InsufficientTicketAvailabilityException excecao) {
                        return false;
                    }
                }));
            }
            inicio.countDown();

            int sucessos = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get()) {
                    sucessos++;
                }
            }
            return new ResultadoConcorrente(sucessos, tentativas - sucessos);
        }
    }

    private TicketBatch prepararLote(int quantidade) {
        prepararUsuario();
        Event evento = prepararEvento();
        return repositorioDeLotes.saveAndFlush(criarLote(evento, "Lote concorrente", quantidade));
    }

    private User prepararUsuario() {
        return repositorioDeUsuarios.saveAndFlush(new User(
                "Cliente Concorrente",
                EMAIL,
                "$2a$10$hashApenasParaTesteDeConcorrencia",
                UserRole.CLIENTE
        ));
    }

    private Event prepararEvento() {
        return repositorioDeEventos.saveAndFlush(new Event(
                "Evento concorrente",
                null,
                "São Paulo",
                OffsetDateTime.parse("2027-10-10T09:00:00-03:00"),
                OffsetDateTime.parse("2027-10-10T18:00:00-03:00")
        ));
    }

    private static TicketBatch criarLote(Event evento, String nome, int quantidade) {
        return new TicketBatch(evento, nome, new BigDecimal("100.00"), quantidade);
    }

    private int quantidadeDisponivel(UUID loteId) {
        return jdbc.queryForObject(
                "SELECT available_quantity FROM ticket_batches WHERE id = ?",
                Integer.class,
                loteId
        );
    }

    private record ResultadoConcorrente(int sucessos, int indisponiveis) {
    }
}
