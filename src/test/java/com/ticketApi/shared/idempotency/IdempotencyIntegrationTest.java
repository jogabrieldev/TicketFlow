package com.ticketApi.shared.idempotency;

import com.ticketApi.event.entity.Event;
import com.ticketApi.event.repository.EventRepository;
import com.ticketApi.order.dto.CreateOrderRequest;
import com.ticketApi.order.dto.OrderResponse;
import com.ticketApi.order.repository.OrderRepository;
import com.ticketApi.order.service.OrderService;
import com.ticketApi.payment.dto.CreatePaymentRequest;
import com.ticketApi.payment.dto.PaymentResponse;
import com.ticketApi.payment.repository.PaymentRepository;
import com.ticketApi.payment.service.PaymentService;
import com.ticketApi.reservation.dto.CreateReservationItemRequest;
import com.ticketApi.reservation.dto.CreateReservationRequest;
import com.ticketApi.reservation.dto.ReservationResponse;
import com.ticketApi.reservation.repository.ReservationRepository;
import com.ticketApi.reservation.service.ReservationService;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "ticketflow.reservation.expiration.enabled=false")
@Testcontainers
class IdempotencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private ReservationService servicoDeReservas;
    @Autowired private OrderService servicoDePedidos;
    @Autowired private PaymentService servicoDePagamentos;
    @Autowired private IdempotencyRepository repositorioDeIdempotencia;
    @Autowired private PaymentRepository repositorioDePagamentos;
    @Autowired private OrderRepository repositorioDePedidos;
    @Autowired private ReservationRepository repositorioDeReservas;
    @Autowired private TicketBatchRepository repositorioDeLotes;
    @Autowired private EventRepository repositorioDeEventos;
    @Autowired private UserRepository repositorioDeUsuarios;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE TABLE idempotency_records, payments, order_items, orders, "
                + "reservation_items, reservations, ticket_batches, events, users CASCADE");
    }

    @Test
    void deveRepetirReservaSemBaixarEstoqueNovamente() {
        CenarioReserva cenario = prepararCenario("reserva-idempotente@ticketflow.com", 10);
        CreateReservationRequest requisicao = requisicao(cenario.loteId(), 2);

        ReservationResponse primeira = servicoDeReservas.criar(cenario.email(), "reserva-001", requisicao);
        ReservationResponse repetida = servicoDeReservas.criar(cenario.email(), "reserva-001", requisicao);

        assertThat(repetida.id()).isEqualTo(primeira.id());
        assertThat(repositorioDeReservas.count()).isEqualTo(1);
        assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(8);
        assertThat(repositorioDeIdempotencia.count()).isEqualTo(1);
    }

    @Test
    void deveRejeitarMesmaChaveComCorpoDiferente() {
        CenarioReserva cenario = prepararCenario("conflito-idempotencia@ticketflow.com", 10);
        servicoDeReservas.criar(cenario.email(), "reserva-002", requisicao(cenario.loteId(), 2));

        assertThatThrownBy(() -> servicoDeReservas.criar(
                cenario.email(),
                "reserva-002",
                requisicao(cenario.loteId(), 3)
        )).isInstanceOf(IdempotencyConflictException.class);

        assertThat(repositorioDeReservas.count()).isEqualTo(1);
        assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(8);
    }

    @Test
    @Timeout(20)
    void deveSerializarRepeticoesConcorrentesDaReserva() throws Exception {
        CenarioReserva cenario = prepararCenario("concorrencia-idempotencia@ticketflow.com", 10);
        CreateReservationRequest requisicao = requisicao(cenario.loteId(), 2);
        CountDownLatch inicio = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<UUID> primeira = executor.submit(() -> criarReservaAposSinal(cenario, requisicao, inicio));
            Future<UUID> segunda = executor.submit(() -> criarReservaAposSinal(cenario, requisicao, inicio));
            inicio.countDown();

            assertThat(primeira.get()).isEqualTo(segunda.get());
            assertThat(repositorioDeReservas.count()).isEqualTo(1);
            assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(8);
        }
    }

    @Test
    void deveRepetirPedidoEPagamentoRetornandoMesmosRecursos() {
        CenarioReserva cenario = prepararCenario("fluxo-idempotente@ticketflow.com", 10);
        ReservationResponse reserva = servicoDeReservas.criar(
                cenario.email(),
                requisicao(cenario.loteId(), 2)
        );
        CreateOrderRequest requisicaoPedido = new CreateOrderRequest(reserva.id());

        OrderResponse primeiroPedido = servicoDePedidos.criar(cenario.email(), "fluxo-001", requisicaoPedido);
        OrderResponse pedidoRepetido = servicoDePedidos.criar(cenario.email(), "fluxo-001", requisicaoPedido);
        CreatePaymentRequest requisicaoPagamento = new CreatePaymentRequest(
                primeiroPedido.id(),
                "tok_aprovado"
        );
        PaymentResponse primeiroPagamento = servicoDePagamentos.criar(
                cenario.email(),
                "fluxo-001",
                requisicaoPagamento
        );
        PaymentResponse pagamentoRepetido = servicoDePagamentos.criar(
                cenario.email(),
                "fluxo-001",
                requisicaoPagamento
        );

        assertThat(pedidoRepetido.id()).isEqualTo(primeiroPedido.id());
        assertThat(pagamentoRepetido.id()).isEqualTo(primeiroPagamento.id());
        assertThat(repositorioDePedidos.count()).isEqualTo(1);
        assertThat(repositorioDePagamentos.count()).isEqualTo(1);
        assertThat(repositorioDeIdempotencia.count()).isEqualTo(2);
    }

    private UUID criarReservaAposSinal(
            CenarioReserva cenario,
            CreateReservationRequest requisicao,
            CountDownLatch inicio
    ) throws InterruptedException {
        inicio.await();
        return servicoDeReservas.criar(cenario.email(), "reserva-concorrente", requisicao).id();
    }

    private CenarioReserva prepararCenario(String email, int quantidade) {
        repositorioDeUsuarios.saveAndFlush(new User("Cliente", email, "hash", UserRole.CLIENTE));
        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        Event evento = repositorioDeEventos.saveAndFlush(
                new Event("Evento", null, "Sao Paulo", inicio, inicio.plusHours(8))
        );
        TicketBatch lote = repositorioDeLotes.saveAndFlush(
                new TicketBatch(evento, "Primeiro lote", new BigDecimal("100.00"), quantidade)
        );
        return new CenarioReserva(email, lote.obterId());
    }

    private static CreateReservationRequest requisicao(UUID loteId, int quantidade) {
        return new CreateReservationRequest(List.of(new CreateReservationItemRequest(loteId, quantidade)));
    }

    private int quantidadeDisponivel(UUID loteId) {
        return jdbc.queryForObject(
                "SELECT available_quantity FROM ticket_batches WHERE id = ?",
                Integer.class,
                loteId
        );
    }

    private record CenarioReserva(String email, UUID loteId) {
    }
}
