package com.ticketApi.reservation.service;

import com.ticketApi.reservation.dto.CreateReservationItemRequest;
import com.ticketApi.reservation.dto.CreateReservationRequest;
import com.ticketApi.reservation.dto.ReservationPageResponse;
import com.ticketApi.reservation.dto.ReservationResponse;
import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.exception.AuthenticatedUserNotFoundException;
import com.ticketApi.reservation.exception.DuplicateTicketBatchException;
import com.ticketApi.reservation.exception.EmptyReservationException;
import com.ticketApi.reservation.exception.MixedEventReservationException;
import com.ticketApi.reservation.repository.ReservationRepository;
import com.ticketApi.shared.idempotency.IdempotencyOperation;
import com.ticketApi.shared.idempotency.IdempotencyService;
import com.ticketApi.shared.idempotency.RequestFingerprint;
import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.ticket.exception.InsufficientTicketAvailabilityException;
import com.ticketApi.ticket.exception.TicketBatchNotFoundException;
import com.ticketApi.ticket.repository.TicketBatchRepository;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository repositorioDeReservas;
    private final TicketBatchRepository repositorioDeLotes;
    private final UserRepository repositorioDeUsuarios;
    private final Duration duracaoDaReserva;
    private final Clock relogio;
    private final IdempotencyService servicoDeIdempotencia;

    public ReservationService(
            ReservationRepository repositorioDeReservas,
            TicketBatchRepository repositorioDeLotes,
            UserRepository repositorioDeUsuarios,
            @Value("${ticketflow.reservation.expiration-duration:PT15M}") Duration duracaoDaReserva,
            Clock relogio,
            IdempotencyService servicoDeIdempotencia
    ) {
        if (duracaoDaReserva == null || duracaoDaReserva.isZero() || duracaoDaReserva.isNegative()) {
            throw new IllegalArgumentException("A duração da reserva deve ser maior que zero");
        }
        this.repositorioDeReservas = repositorioDeReservas;
        this.repositorioDeLotes = repositorioDeLotes;
        this.repositorioDeUsuarios = repositorioDeUsuarios;
        this.duracaoDaReserva = duracaoDaReserva;
        this.relogio = relogio;
        this.servicoDeIdempotencia = servicoDeIdempotencia;
    }

    @Transactional
    public ReservationResponse criar(String emailDoUsuario, CreateReservationRequest requisicao) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        validarReservaComItens(requisicao);
        validarLotesDuplicados(requisicao.itens());

        return criarNova(usuario, requisicao);
    }

    @Transactional
    public ReservationResponse criar(
            String emailDoUsuario,
            String chaveDeIdempotencia,
            CreateReservationRequest requisicao
    ) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        validarReservaComItens(requisicao);
        validarLotesDuplicados(requisicao.itens());
        String hash = RequestFingerprint.gerar(representacaoCanonica(requisicao));

        return servicoDeIdempotencia.executar(
                usuario.obterId(),
                IdempotencyOperation.CRIAR_RESERVA,
                chaveDeIdempotencia,
                hash,
                recursoId -> repositorioDeReservas.buscarComItensPorId(recursoId)
                        .map(ReservationResponse::de)
                        .orElseThrow(() -> new IllegalStateException("Reserva idempotente não encontrada")),
                () -> {
                    ReservationResponse resposta = criarNova(usuario, requisicao);
                    return new IdempotencyService.CreatedResource<>(resposta.id(), resposta);
                }
        );
    }

    private ReservationResponse criarNova(User usuario, CreateReservationRequest requisicao) {

        Map<UUID, TicketBatch> lotesPorId = buscarLotes(requisicao.itens());
        validarMesmoEvento(requisicao.itens(), lotesPorId);

        OffsetDateTime agora = OffsetDateTime.now(relogio);
        Reservation reserva = new Reservation(usuario, agora.plus(duracaoDaReserva), agora);

        List<CreateReservationItemRequest> itensOrdenados = requisicao.itens().stream()
                .sorted(Comparator.comparing(CreateReservationItemRequest::loteId))
                .toList();
        for (CreateReservationItemRequest item : itensOrdenados) {
            int lotesAtualizados = repositorioDeLotes.reservarSeDisponivel(
                    item.loteId(),
                    item.quantidade(),
                    agora
            );
            if (lotesAtualizados == 0) {
                throw new InsufficientTicketAvailabilityException(item.loteId(), item.quantidade());
            }
        }

        for (CreateReservationItemRequest item : requisicao.itens()) {
            TicketBatch lote = lotesPorId.get(item.loteId());
            reserva.adicionarItem(lote, item.quantidade());
        }

        return ReservationResponse.de(repositorioDeReservas.saveAndFlush(reserva));
    }

    private static String representacaoCanonica(CreateReservationRequest requisicao) {
        return requisicao.itens().stream()
                .sorted(Comparator.comparing(CreateReservationItemRequest::loteId))
                .map(item -> item.loteId() + ":" + item.quantidade())
                .collect(java.util.stream.Collectors.joining("|"));
    }

    @Transactional(readOnly = true)
    public ReservationPageResponse listarDoUsuario(String emailDoUsuario, int pagina, int tamanho) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        PageRequest paginacao = PageRequest.of(
                pagina,
                tamanho,
                Sort.by(Sort.Direction.DESC, "criadoEm")
        );
        Page<ReservationResponse> reservas = repositorioDeReservas
                .buscarPorUsuarioId(usuario.obterId(), paginacao)
                .map(ReservationResponse::de);

        return ReservationPageResponse.de(reservas);
    }

    private User buscarUsuarioAutenticado(String email) {
        return repositorioDeUsuarios.buscarPorEmail(email)
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(email));
    }

    private static void validarReservaComItens(CreateReservationRequest requisicao) {
        if (requisicao == null || requisicao.itens() == null || requisicao.itens().isEmpty()) {
            throw new EmptyReservationException();
        }
    }

    private static void validarLotesDuplicados(List<CreateReservationItemRequest> itens) {
        Set<UUID> identificadores = new HashSet<>();
        for (CreateReservationItemRequest item : itens) {
            if (!identificadores.add(item.loteId())) {
                throw new DuplicateTicketBatchException(item.loteId());
            }
        }
    }

    private Map<UUID, TicketBatch> buscarLotes(List<CreateReservationItemRequest> itens) {
        List<UUID> identificadores = itens.stream().map(CreateReservationItemRequest::loteId).toList();
        Map<UUID, TicketBatch> lotesPorId = new HashMap<>();
        repositorioDeLotes.findAllById(identificadores)
                .forEach(lote -> lotesPorId.put(lote.obterId(), lote));

        for (UUID loteId : identificadores) {
            if (!lotesPorId.containsKey(loteId)) {
                throw new TicketBatchNotFoundException(loteId);
            }
        }
        return lotesPorId;
    }

    private static void validarMesmoEvento(
            List<CreateReservationItemRequest> itens,
            Map<UUID, TicketBatch> lotesPorId
    ) {
        UUID eventoId = lotesPorId.get(itens.getFirst().loteId()).obterEvento().obterId();
        boolean possuiOutroEvento = itens.stream()
                .map(item -> lotesPorId.get(item.loteId()).obterEvento().obterId())
                .anyMatch(id -> !id.equals(eventoId));
        if (possuiOutroEvento) {
            throw new MixedEventReservationException();
        }
    }
}
