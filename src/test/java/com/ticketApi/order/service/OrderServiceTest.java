package com.ticketApi.order.service;

import com.ticketApi.event.entity.Event;
import com.ticketApi.order.dto.CreateOrderRequest;
import com.ticketApi.order.dto.OrderPageResponse;
import com.ticketApi.order.dto.OrderResponse;
import com.ticketApi.order.entity.Order;
import com.ticketApi.order.entity.OrderStatus;
import com.ticketApi.order.exception.OrderAlreadyExistsException;
import com.ticketApi.order.exception.ReservationForOrderNotFoundException;
import com.ticketApi.order.exception.ReservationUnavailableForOrderException;
import com.ticketApi.order.repository.OrderRepository;
import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.repository.ReservationRepository;
import com.ticketApi.shared.idempotency.IdempotencyService;
import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2030-01-01T12:00:00Z");
    private static final String EMAIL = "cliente@ticketflow.com";

    @Mock
    private OrderRepository repositorioDePedidos;

    @Mock
    private ReservationRepository repositorioDeReservas;

    @Mock
    private UserRepository repositorioDeUsuarios;

    @Mock
    private IdempotencyService servicoDeIdempotencia;

    private OrderService servicoDePedidos;
    private User usuario;
    private Reservation reserva;

    @BeforeEach
    void preparar() {
        Clock relogio = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);
        servicoDePedidos = new OrderService(
                repositorioDePedidos,
                repositorioDeReservas,
                repositorioDeUsuarios,
                relogio,
                servicoDeIdempotencia
        );
        usuario = new User("Cliente", EMAIL, "hash", UserRole.CLIENTE);
        reserva = criarReserva(usuario, AGORA.plusMinutes(15));
    }

    @Test
    void deveCriarPedidoComSnapshotEValorCalculadoNoServidor() {
        prepararCriacaoValida();
        given(repositorioDePedidos.saveAndFlush(any(Order.class))).willAnswer(invocacao -> invocacao.getArgument(0));

        OrderResponse resposta = servicoDePedidos.criar(EMAIL, new CreateOrderRequest(reserva.obterId()));

        assertThat(resposta.status()).isEqualTo(OrderStatus.PENDENTE_PAGAMENTO);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("350.00");
        assertThat(resposta.limitePagamento()).isEqualTo(reserva.obterExpiraEm());
        assertThat(resposta.itens()).hasSize(2);
        assertThat(resposta.itens()).extracting(item -> item.subtotal().toPlainString())
                .containsExactly("200.00", "150.00");
    }

    @Test
    void deveBloquearReservaAntesDeCriarPedido() {
        prepararCriacaoValida();
        given(repositorioDePedidos.saveAndFlush(any(Order.class))).willAnswer(invocacao -> invocacao.getArgument(0));

        servicoDePedidos.criar(EMAIL, new CreateOrderRequest(reserva.obterId()));

        verify(repositorioDeReservas).buscarPorIdParaAtualizacao(reserva.obterId());
    }

    @Test
    void deveOcultarReservaDeOutroUsuario() {
        User outroUsuario = new User("Outro", "outro@ticketflow.com", "hash", UserRole.CLIENTE);
        Reservation reservaDeOutroUsuario = criarReserva(outroUsuario, AGORA.plusMinutes(15));
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repositorioDeReservas.buscarPorIdParaAtualizacao(reservaDeOutroUsuario.obterId()))
                .willReturn(Optional.of(reservaDeOutroUsuario));

        assertThatThrownBy(() -> servicoDePedidos.criar(
                EMAIL,
                new CreateOrderRequest(reservaDeOutroUsuario.obterId())
        )).isInstanceOf(ReservationForOrderNotFoundException.class);
    }

    @Test
    void deveRejeitarReservaExpirada() {
        reserva = criarReserva(usuario, AGORA.minusSeconds(1));
        prepararReservaEncontrada();

        assertThatThrownBy(() -> servicoDePedidos.criar(EMAIL, new CreateOrderRequest(reserva.obterId())))
                .isInstanceOf(ReservationUnavailableForOrderException.class);
    }

    @Test
    void deveRejeitarSegundoPedidoDaMesmaReserva() {
        prepararReservaEncontrada();
        given(repositorioDePedidos.existsByReservaId(reserva.obterId())).willReturn(true);

        assertThatThrownBy(() -> servicoDePedidos.criar(EMAIL, new CreateOrderRequest(reserva.obterId())))
                .isInstanceOf(OrderAlreadyExistsException.class);
    }

    @Test
    void deveListarSomentePedidosDoUsuarioComPaginacao() {
        Order pedido = new Order(usuario, reserva);
        PageImpl<Order> pagina = new PageImpl<>(List.of(pedido));
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repositorioDePedidos.buscarPorUsuarioId(any(), any(Pageable.class))).willReturn(pagina);
        given(repositorioDePedidos.buscarComItensPorIds(List.of(pedido.obterId()))).willReturn(List.of(pedido));

        OrderPageResponse resposta = servicoDePedidos.listarDoUsuario(EMAIL, 0, 20);

        assertThat(resposta.conteudo()).hasSize(1);
        assertThat(resposta.conteudo().getFirst().id()).isEqualTo(pedido.obterId());
        ArgumentCaptor<Pageable> paginacao = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorioDePedidos).buscarPorUsuarioId(any(), paginacao.capture());
        assertThat(paginacao.getValue().getPageSize()).isEqualTo(20);
    }

    private void prepararCriacaoValida() {
        prepararReservaEncontrada();
        given(repositorioDePedidos.existsByReservaId(reserva.obterId())).willReturn(false);
    }

    private void prepararReservaEncontrada() {
        given(repositorioDeUsuarios.buscarPorEmail(EMAIL)).willReturn(Optional.of(usuario));
        given(repositorioDeReservas.buscarPorIdParaAtualizacao(reserva.obterId())).willReturn(Optional.of(reserva));
        given(repositorioDeReservas.buscarComItensPorId(reserva.obterId())).willReturn(Optional.of(reserva));
    }

    private static Reservation criarReserva(User usuario, OffsetDateTime expiraEm) {
        Event evento = new Event(
                "Evento",
                null,
                "Sao Paulo",
                AGORA.plusDays(10),
                AGORA.plusDays(10).plusHours(8)
        );
        TicketBatch primeiroLote = new TicketBatch(evento, "Primeiro lote", new BigDecimal("100.00"), 10);
        TicketBatch segundoLote = new TicketBatch(evento, "Segundo lote", new BigDecimal("150.00"), 10);
        Reservation resultado = new Reservation(usuario, expiraEm, AGORA.minusMinutes(15));
        resultado.adicionarItem(primeiroLote, 2);
        resultado.adicionarItem(segundoLote, 1);
        return resultado;
    }
}
