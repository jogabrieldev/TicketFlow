package com.ticketApi.order.service;

import com.ticketApi.event.entity.Event;
import com.ticketApi.event.repository.EventRepository;
import com.ticketApi.order.dto.CreateOrderRequest;
import com.ticketApi.order.dto.OrderPageResponse;
import com.ticketApi.order.dto.OrderResponse;
import com.ticketApi.order.entity.OrderStatus;
import com.ticketApi.order.exception.OrderAlreadyExistsException;
import com.ticketApi.order.exception.ReservationUnavailableForOrderException;
import com.ticketApi.order.repository.OrderRepository;
import com.ticketApi.reservation.dto.CreateReservationItemRequest;
import com.ticketApi.reservation.dto.CreateReservationRequest;
import com.ticketApi.reservation.dto.ReservationResponse;
import com.ticketApi.reservation.service.ReservationExpirationService;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "ticketflow.reservation.expiration.enabled=false")
@Testcontainers
class OrderIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private OrderService servicoDePedidos;

    @Autowired
    private ReservationService servicoDeReservas;

    @Autowired
    private ReservationExpirationService servicoDeExpiracao;

    @Autowired
    private OrderRepository repositorioDePedidos;

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
        jdbc.execute("TRUNCATE TABLE order_items, orders, reservation_items, reservations, "
                + "ticket_batches, events, users CASCADE");
    }

    @Test
    void devePersistirPedidoComSnapshotFinanceiro() {
        Cenario cenario = prepararReserva("cliente@ticketflow.com", 10, 2);

        OrderResponse pedido = servicoDePedidos.criar(
                cenario.email(),
                new CreateOrderRequest(cenario.reserva().id())
        );

        assertThat(pedido.status()).isEqualTo(OrderStatus.PENDENTE_PAGAMENTO);
        assertThat(pedido.valorTotal()).isEqualByComparingTo("200.00");
        assertThat(pedido.itens()).singleElement().satisfies(item -> {
            assertThat(item.quantidade()).isEqualTo(2);
            assertThat(item.precoUnitario()).isEqualByComparingTo("100.00");
            assertThat(item.subtotal()).isEqualByComparingTo("200.00");
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class)).isEqualTo(1);
    }

    @Test
    void deveImpedirDoisPedidosParaMesmaReserva() {
        Cenario cenario = prepararReserva("duplicado@ticketflow.com", 10, 1);
        CreateOrderRequest requisicao = new CreateOrderRequest(cenario.reserva().id());
        servicoDePedidos.criar(cenario.email(), requisicao);

        assertThatThrownBy(() -> servicoDePedidos.criar(cenario.email(), requisicao))
                .isInstanceOf(OrderAlreadyExistsException.class);
        assertThat(repositorioDePedidos.count()).isEqualTo(1);
    }

    @Test
    void deveListarSomentePedidosDoUsuarioAutenticado() {
        Cenario primeiro = prepararReserva("primeiro@ticketflow.com", 10, 1);
        Cenario segundo = prepararReserva("segundo@ticketflow.com", 10, 1);
        servicoDePedidos.criar(primeiro.email(), new CreateOrderRequest(primeiro.reserva().id()));
        servicoDePedidos.criar(segundo.email(), new CreateOrderRequest(segundo.reserva().id()));

        OrderPageResponse pagina = servicoDePedidos.listarDoUsuario(primeiro.email(), 0, 20);

        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.conteudo()).singleElement()
                .satisfies(pedido -> assertThat(pedido.reservaId()).isEqualTo(primeiro.reserva().id()));
    }

    @Test
    void deveCancelarPedidoQuandoReservaExpirar() {
        Cenario cenario = prepararReserva("expirado@ticketflow.com", 10, 3);
        OrderResponse pedido = servicoDePedidos.criar(
                cenario.email(),
                new CreateOrderRequest(cenario.reserva().id())
        );
        OffsetDateTime vencimento = cenario.reserva().criadoEm().plusSeconds(1);
        OffsetDateTime agora = vencimento.plusSeconds(1);
        definirExpiracao(cenario.reserva().id(), vencimento);

        boolean expirou = servicoDeExpiracao.expirarSeNecessario(cenario.reserva().id(), agora);

        assertThat(expirou).isTrue();
        assertThat(repositorioDePedidos.findById(pedido.id()).orElseThrow().obterStatus())
                .isEqualTo(OrderStatus.CANCELADO);
        assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(10);
    }

    @Test
    @Timeout(20)
    void deveCriarSomenteUmPedidoEmDuasTentativasConcorrentes() throws Exception {
        Cenario cenario = prepararReserva("concorrente@ticketflow.com", 10, 1);
        CreateOrderRequest requisicao = new CreateOrderRequest(cenario.reserva().id());
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primeira = executor.submit(() -> tentarCriarPedido(inicio, cenario.email(), requisicao));
            Future<Boolean> segunda = executor.submit(() -> tentarCriarPedido(inicio, cenario.email(), requisicao));
            inicio.countDown();

            assertThat(List.of(obter(primeira), obter(segunda))).containsExactlyInAnyOrder(true, false);
            assertThat(repositorioDePedidos.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(20)
    void deveManterConsistenciaNaConcorrenciaEntrePedidoEExpiracao() throws Exception {
        Cenario cenario = prepararReserva("corrida@ticketflow.com", 10, 2);
        OffsetDateTime instanteDeExpiracao = cenario.reserva().expiraEm().plusSeconds(1);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> criacao = executor.submit(() -> tentarCriarPedido(
                    inicio,
                    cenario.email(),
                    new CreateOrderRequest(cenario.reserva().id())
            ));
            Future<Boolean> expiracao = executor.submit(() -> {
                inicio.await();
                return servicoDeExpiracao.expirarSeNecessario(
                        cenario.reserva().id(),
                        instanteDeExpiracao
                );
            });
            inicio.countDown();

            obter(criacao);
            assertThat(obter(expiracao)).isTrue();
            assertThat(quantidadeDisponivel(cenario.loteId())).isEqualTo(10);
            repositorioDePedidos.findAll().forEach(pedido ->
                    assertThat(pedido.obterStatus()).isEqualTo(OrderStatus.CANCELADO)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean tentarCriarPedido(
            CountDownLatch inicio,
            String email,
            CreateOrderRequest requisicao
    ) throws InterruptedException {
        inicio.await();
        try {
            servicoDePedidos.criar(email, requisicao);
            return true;
        } catch (OrderAlreadyExistsException | ReservationUnavailableForOrderException excecaoEsperada) {
            return false;
        }
    }

    private static boolean obter(Future<Boolean> resultado) throws Exception {
        try {
            return resultado.get();
        } catch (ExecutionException excecao) {
            Throwable causa = excecao.getCause();
            if (causa instanceof Exception falha) {
                throw falha;
            }
            throw excecao;
        }
    }

    private Cenario prepararReserva(String email, int quantidadeTotal, int quantidadeReservada) {
        User usuario = repositorioDeUsuarios.saveAndFlush(
                new User("Cliente", email, "hash", UserRole.CLIENTE)
        );
        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        Event evento = repositorioDeEventos.saveAndFlush(
                new Event("Evento", null, "Sao Paulo", inicio, inicio.plusHours(8))
        );
        TicketBatch lote = repositorioDeLotes.saveAndFlush(
                new TicketBatch(evento, "Primeiro lote", new BigDecimal("100.00"), quantidadeTotal)
        );
        ReservationResponse reserva = servicoDeReservas.criar(
                usuario.obterEmail(),
                new CreateReservationRequest(List.of(
                        new CreateReservationItemRequest(lote.obterId(), quantidadeReservada)
                ))
        );
        return new Cenario(usuario.obterEmail(), reserva, lote.obterId());
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

    private record Cenario(String email, ReservationResponse reserva, UUID loteId) {
    }
}
