package com.ticketApi.payment.service;

import com.ticketApi.event.entity.Event;
import com.ticketApi.event.repository.EventRepository;
import com.ticketApi.order.dto.CreateOrderRequest;
import com.ticketApi.order.dto.OrderResponse;
import com.ticketApi.order.entity.OrderStatus;
import com.ticketApi.order.repository.OrderRepository;
import com.ticketApi.order.service.OrderService;
import com.ticketApi.organization.repository.OrganizationRepository;
import com.ticketApi.payment.dto.CreatePaymentRequest;
import com.ticketApi.payment.dto.PaymentResponse;
import com.ticketApi.payment.entity.PaymentStatus;
import com.ticketApi.payment.exception.PaymentUnavailableException;
import com.ticketApi.payment.repository.PaymentRepository;
import com.ticketApi.reservation.dto.CreateReservationItemRequest;
import com.ticketApi.reservation.dto.CreateReservationRequest;
import com.ticketApi.reservation.dto.ReservationResponse;
import com.ticketApi.reservation.entity.ReservationStatus;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.ticketApi.organization.OrganizationTestFactory.criarOrganizacao;

@SpringBootTest(properties = "ticketflow.reservation.expiration.enabled=false")
@Testcontainers
class PaymentIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private PaymentService servicoDePagamentos;
    @Autowired private OrderService servicoDePedidos;
    @Autowired private ReservationService servicoDeReservas;
    @Autowired private PaymentRepository repositorioDePagamentos;
    @Autowired private OrderRepository repositorioDePedidos;
    @Autowired private ReservationRepository repositorioDeReservas;
    @Autowired private UserRepository repositorioDeUsuarios;
    @Autowired private EventRepository repositorioDeEventos;
    @Autowired private OrganizationRepository repositorioDeOrganizacoes;
    @Autowired private TicketBatchRepository repositorioDeLotes;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE TABLE payments, order_items, orders, reservation_items, reservations, "
                + "ticket_batches, events, organization_members, organizations, users CASCADE");
    }

    @Test
    void deveAprovarPagamentoEConfirmarPedidoEReserva() {
        Cenario cenario = prepararPedido("aprovado@ticketflow.com");

        PaymentResponse pagamento = servicoDePagamentos.criar(
                cenario.email(),
                new CreatePaymentRequest(cenario.pedido().id(), "tok_aprovado")
        );

        assertThat(pagamento.status()).isEqualTo(PaymentStatus.APROVADO);
        assertThat(pagamento.valor()).isEqualByComparingTo("200.00");
        assertThat(repositorioDePedidos.findById(cenario.pedido().id()).orElseThrow().obterStatus())
                .isEqualTo(OrderStatus.PAGO);
        assertThat(repositorioDeReservas.findById(cenario.reserva().id()).orElseThrow().obterStatus())
                .isEqualTo(ReservationStatus.CONFIRMADA);
    }

    @Test
    void devePermitirNovaTentativaDepoisDeRecusa() {
        Cenario cenario = prepararPedido("recusado@ticketflow.com");

        PaymentResponse recusado = servicoDePagamentos.criar(
                cenario.email(),
                new CreatePaymentRequest(cenario.pedido().id(), "tok_recusado")
        );
        PaymentResponse aprovado = servicoDePagamentos.criar(
                cenario.email(),
                new CreatePaymentRequest(cenario.pedido().id(), "tok_aprovado")
        );

        assertThat(recusado.status()).isEqualTo(PaymentStatus.RECUSADO);
        assertThat(aprovado.status()).isEqualTo(PaymentStatus.APROVADO);
        assertThat(repositorioDePagamentos.count()).isEqualTo(2);
    }

    @Test
    void deveBloquearNovaTentativaQuandoPagamentoEstiverProcessando() {
        Cenario cenario = prepararPedido("processando@ticketflow.com");
        servicoDePagamentos.criar(
                cenario.email(),
                new CreatePaymentRequest(cenario.pedido().id(), "tok_processando")
        );

        assertThatThrownBy(() -> servicoDePagamentos.criar(
                cenario.email(),
                new CreatePaymentRequest(cenario.pedido().id(), "tok_aprovado")
        )).isInstanceOf(PaymentUnavailableException.class);
    }

    @Test
    @Timeout(20)
    void deveCriarSomenteUmPagamentoAtivoEmTentativasConcorrentes() throws Exception {
        Cenario cenario = prepararPedido("concorrente-pagamento@ticketflow.com");
        CountDownLatch inicio = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> primeira = executor.submit(() -> tentarPagar(cenario, inicio));
            Future<Boolean> segunda = executor.submit(() -> tentarPagar(cenario, inicio));
            inicio.countDown();

            assertThat(List.of(primeira.get(), segunda.get())).containsExactlyInAnyOrder(true, false);
            assertThat(repositorioDePagamentos.count()).isEqualTo(1);
        }
    }

    private boolean tentarPagar(Cenario cenario, CountDownLatch inicio) throws InterruptedException {
        inicio.await();
        try {
            servicoDePagamentos.criar(
                    cenario.email(),
                    new CreatePaymentRequest(cenario.pedido().id(), "tok_aprovado")
            );
            return true;
        } catch (PaymentUnavailableException excecaoEsperada) {
            return false;
        }
    }

    private Cenario prepararPedido(String email) {
        User usuario = repositorioDeUsuarios.saveAndFlush(new User("Cliente", email, "hash", UserRole.CLIENTE));
        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        Event evento = repositorioDeEventos.saveAndFlush(
                new Event(
                        repositorioDeOrganizacoes.saveAndFlush(criarOrganizacao()),
                        "Evento", null, "Sao Paulo", inicio, inicio.plusHours(8))
        );
        TicketBatch lote = repositorioDeLotes.saveAndFlush(
                new TicketBatch(evento, "Primeiro lote", new BigDecimal("100.00"), 10)
        );
        ReservationResponse reserva = servicoDeReservas.criar(
                email,
                new CreateReservationRequest(List.of(new CreateReservationItemRequest(lote.obterId(), 2)))
        );
        OrderResponse pedido = servicoDePedidos.criar(email, new CreateOrderRequest(reserva.id()));
        return new Cenario(email, reserva, pedido);
    }

    private record Cenario(String email, ReservationResponse reserva, OrderResponse pedido) {
    }
}
