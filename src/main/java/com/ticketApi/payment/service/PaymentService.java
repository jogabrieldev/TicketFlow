package com.ticketApi.payment.service;

import com.ticketApi.order.entity.Order;
import com.ticketApi.order.entity.OrderStatus;
import com.ticketApi.order.repository.OrderRepository;
import com.ticketApi.payment.dto.CreatePaymentRequest;
import com.ticketApi.payment.dto.PaymentResponse;
import com.ticketApi.payment.entity.Payment;
import com.ticketApi.payment.entity.PaymentStatus;
import com.ticketApi.payment.exception.PaymentNotFoundException;
import com.ticketApi.payment.exception.PaymentOrderNotFoundException;
import com.ticketApi.payment.exception.PaymentUnavailableException;
import com.ticketApi.payment.gateway.PaymentGateway;
import com.ticketApi.payment.gateway.PaymentGatewayResult;
import com.ticketApi.payment.repository.PaymentRepository;
import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.entity.ReservationStatus;
import com.ticketApi.reservation.exception.AuthenticatedUserNotFoundException;
import com.ticketApi.reservation.repository.ReservationRepository;
import com.ticketApi.shared.idempotency.IdempotencyOperation;
import com.ticketApi.shared.idempotency.IdempotencyService;
import com.ticketApi.shared.idempotency.RequestFingerprint;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Set;

@Service
public class PaymentService {

    private static final Set<PaymentStatus> STATUS_ATIVOS = Set.of(
            PaymentStatus.PROCESSANDO,
            PaymentStatus.APROVADO
    );

    private final PaymentRepository repositorioDePagamentos;
    private final OrderRepository repositorioDePedidos;
    private final ReservationRepository repositorioDeReservas;
    private final UserRepository repositorioDeUsuarios;
    private final PaymentGateway gatewayDePagamento;
    private final Clock relogio;
    private final IdempotencyService servicoDeIdempotencia;

    public PaymentService(
            PaymentRepository repositorioDePagamentos,
            OrderRepository repositorioDePedidos,
            ReservationRepository repositorioDeReservas,
            UserRepository repositorioDeUsuarios,
            PaymentGateway gatewayDePagamento,
            Clock relogio,
            IdempotencyService servicoDeIdempotencia
    ) {
        this.repositorioDePagamentos = repositorioDePagamentos;
        this.repositorioDePedidos = repositorioDePedidos;
        this.repositorioDeReservas = repositorioDeReservas;
        this.repositorioDeUsuarios = repositorioDeUsuarios;
        this.gatewayDePagamento = gatewayDePagamento;
        this.relogio = relogio;
        this.servicoDeIdempotencia = servicoDeIdempotencia;
    }

    @Transactional
    public PaymentResponse criar(String emailDoUsuario, CreatePaymentRequest requisicao) {
        User usuario = buscarUsuario(emailDoUsuario);
        return criarNovo(usuario, requisicao);
    }

    @Transactional
    public PaymentResponse criar(
            String emailDoUsuario,
            String chaveDeIdempotencia,
            CreatePaymentRequest requisicao
    ) {
        User usuario = buscarUsuario(emailDoUsuario);
        String hash = RequestFingerprint.gerar(
                requisicao.pedidoId() + "|" + requisicao.tokenPagamento()
        );
        return servicoDeIdempotencia.executar(
                usuario.obterId(),
                IdempotencyOperation.CRIAR_PAGAMENTO,
                chaveDeIdempotencia,
                hash,
                recursoId -> repositorioDePagamentos.findById(recursoId)
                        .map(PaymentResponse::de)
                        .orElseThrow(() -> new IllegalStateException("Pagamento idempotente não encontrado")),
                () -> {
                    PaymentResponse resposta = criarNovo(usuario, requisicao);
                    return new IdempotencyService.CreatedResource<>(resposta.id(), resposta);
                }
        );
    }

    private PaymentResponse criarNovo(User usuario, CreatePaymentRequest requisicao) {
        Order pedidoInicial = repositorioDePedidos.findById(requisicao.pedidoId())
                .filter(pedido -> pedido.obterUsuario().obterId().equals(usuario.obterId()))
                .orElseThrow(() -> new PaymentOrderNotFoundException(requisicao.pedidoId()));

        Reservation reserva = repositorioDeReservas
                .buscarPorIdParaAtualizacao(pedidoInicial.obterReserva().obterId())
                .orElseThrow(() -> new PaymentUnavailableException(requisicao.pedidoId()));
        Order pedido = repositorioDePedidos.buscarPorIdParaAtualizacao(requisicao.pedidoId())
                .filter(encontrado -> encontrado.obterUsuario().obterId().equals(usuario.obterId()))
                .orElseThrow(() -> new PaymentOrderNotFoundException(requisicao.pedidoId()));

        OffsetDateTime agora = OffsetDateTime.now(relogio);
        validarDisponibilidade(pedido, reserva, agora);
        if (repositorioDePagamentos.existsByPedidoIdAndStatusIn(pedido.obterId(), STATUS_ATIVOS)) {
            throw new PaymentUnavailableException(pedido.obterId());
        }

        PaymentGatewayResult resultado = gatewayDePagamento.processar(
                pedido.obterId(),
                pedido.obterValorTotal(),
                requisicao.tokenPagamento()
        );
        Payment pagamento = new Payment(pedido, resultado.status(), resultado.referencia());
        if (resultado.status() == PaymentStatus.APROVADO) {
            reserva.confirmar();
            pedido.marcarComoPago();
        }
        return PaymentResponse.de(repositorioDePagamentos.saveAndFlush(pagamento));
    }

    @Transactional(readOnly = true)
    public PaymentResponse buscar(String emailDoUsuario, java.util.UUID pagamentoId) {
        User usuario = buscarUsuario(emailDoUsuario);
        return repositorioDePagamentos.buscarDoUsuario(pagamentoId, usuario.obterId())
                .map(PaymentResponse::de)
                .orElseThrow(() -> new PaymentNotFoundException(pagamentoId));
    }

    private static void validarDisponibilidade(Order pedido, Reservation reserva, OffsetDateTime agora) {
        boolean indisponivel = pedido.obterStatus() != OrderStatus.PENDENTE_PAGAMENTO
                || !pedido.obterLimitePagamento().isAfter(agora)
                || reserva.obterStatus() != ReservationStatus.PENDENTE
                || !reserva.obterExpiraEm().isAfter(agora);
        if (indisponivel) {
            throw new PaymentUnavailableException(pedido.obterId());
        }
    }

    private User buscarUsuario(String email) {
        return repositorioDeUsuarios.buscarPorEmail(email)
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(email));
    }
}
