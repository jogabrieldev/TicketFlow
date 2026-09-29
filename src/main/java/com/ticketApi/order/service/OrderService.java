package com.ticketApi.order.service;

import com.ticketApi.order.dto.CreateOrderRequest;
import com.ticketApi.order.dto.OrderPageResponse;
import com.ticketApi.order.dto.OrderResponse;
import com.ticketApi.order.entity.Order;
import com.ticketApi.order.exception.OrderAlreadyExistsException;
import com.ticketApi.order.exception.ReservationForOrderNotFoundException;
import com.ticketApi.order.exception.ReservationUnavailableForOrderException;
import com.ticketApi.order.repository.OrderRepository;
import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.entity.ReservationStatus;
import com.ticketApi.reservation.exception.AuthenticatedUserNotFoundException;
import com.ticketApi.reservation.repository.ReservationRepository;
import com.ticketApi.shared.idempotency.IdempotencyOperation;
import com.ticketApi.shared.idempotency.IdempotencyService;
import com.ticketApi.shared.idempotency.RequestFingerprint;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repositorioDePedidos;
    private final ReservationRepository repositorioDeReservas;
    private final UserRepository repositorioDeUsuarios;
    private final Clock relogio;
    private final IdempotencyService servicoDeIdempotencia;

    public OrderService(
            OrderRepository repositorioDePedidos,
            ReservationRepository repositorioDeReservas,
            UserRepository repositorioDeUsuarios,
            Clock relogio,
            IdempotencyService servicoDeIdempotencia
    ) {
        this.repositorioDePedidos = repositorioDePedidos;
        this.repositorioDeReservas = repositorioDeReservas;
        this.repositorioDeUsuarios = repositorioDeUsuarios;
        this.relogio = relogio;
        this.servicoDeIdempotencia = servicoDeIdempotencia;
    }

    @Transactional
    public OrderResponse criar(String emailDoUsuario, CreateOrderRequest requisicao) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        return criarNovo(usuario, requisicao);
    }

    @Transactional
    public OrderResponse criar(
            String emailDoUsuario,
            String chaveDeIdempotencia,
            CreateOrderRequest requisicao
    ) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        String hash = RequestFingerprint.gerar(requisicao.reservaId().toString());
        return servicoDeIdempotencia.executar(
                usuario.obterId(),
                IdempotencyOperation.CRIAR_PEDIDO,
                chaveDeIdempotencia,
                hash,
                recursoId -> repositorioDePedidos.buscarComItensPorId(recursoId)
                        .map(OrderResponse::de)
                        .orElseThrow(() -> new IllegalStateException("Pedido idempotente não encontrado")),
                () -> {
                    OrderResponse resposta = criarNovo(usuario, requisicao);
                    return new IdempotencyService.CreatedResource<>(resposta.id(), resposta);
                }
        );
    }

    private OrderResponse criarNovo(User usuario, CreateOrderRequest requisicao) {
        Reservation reservaBloqueada = repositorioDeReservas.buscarPorIdParaAtualizacao(requisicao.reservaId())
                .orElseThrow(() -> new ReservationForOrderNotFoundException(requisicao.reservaId()));

        if (!reservaBloqueada.obterUsuario().obterId().equals(usuario.obterId())) {
            throw new ReservationForOrderNotFoundException(requisicao.reservaId());
        }

        Reservation reserva = repositorioDeReservas.buscarComItensPorId(requisicao.reservaId())
                .orElseThrow(() -> new ReservationForOrderNotFoundException(requisicao.reservaId()));
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        if (reserva.obterStatus() != ReservationStatus.PENDENTE || !reserva.obterExpiraEm().isAfter(agora)) {
            throw new ReservationUnavailableForOrderException(reserva.obterId());
        }
        if (repositorioDePedidos.existsByReservaId(reserva.obterId())) {
            throw new OrderAlreadyExistsException(reserva.obterId());
        }

        return OrderResponse.de(repositorioDePedidos.saveAndFlush(new Order(usuario, reserva)));
    }

    @Transactional(readOnly = true)
    public OrderPageResponse listarDoUsuario(String emailDoUsuario, int pagina, int tamanho) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        Page<Order> pedidos = repositorioDePedidos.buscarPorUsuarioId(
                usuario.obterId(),
                PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm"))
        );
        List<java.util.UUID> identificadores = pedidos.stream().map(Order::obterId).toList();
        if (!identificadores.isEmpty()) {
            repositorioDePedidos.buscarComItensPorIds(identificadores);
        }
        return OrderPageResponse.de(pedidos.map(OrderResponse::de));
    }

    private User buscarUsuarioAutenticado(String email) {
        return repositorioDeUsuarios.buscarPorEmail(email)
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(email));
    }
}
