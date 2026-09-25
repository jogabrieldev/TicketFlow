package com.ticketApi.ticket.service;

import com.ticketApi.event.entity.Event;
import com.ticketApi.event.exception.EventNotFoundException;
import com.ticketApi.event.repository.EventRepository;
import com.ticketApi.ticket.dto.CreateTicketBatchRequest;
import com.ticketApi.ticket.dto.TicketBatchPageResponse;
import com.ticketApi.ticket.dto.TicketBatchResponse;
import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.ticket.exception.TicketBatchNotFoundException;
import com.ticketApi.ticket.repository.TicketBatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TicketBatchService {

    private final TicketBatchRepository repositorioDeLotes;
    private final EventRepository repositorioDeEventos;

    public TicketBatchService(TicketBatchRepository repositorioDeLotes, EventRepository repositorioDeEventos) {
        this.repositorioDeLotes = repositorioDeLotes;
        this.repositorioDeEventos = repositorioDeEventos;
    }

    @Transactional
    public TicketBatchResponse criar(UUID eventoId, CreateTicketBatchRequest requisicao) {
        Event evento = repositorioDeEventos.findById(eventoId).orElseThrow(() -> new EventNotFoundException(eventoId));
        TicketBatch lote = new TicketBatch(
                evento,
                requisicao.nome(),
                requisicao.preco(),
                requisicao.quantidadeTotal()
        );

        return TicketBatchResponse.de(repositorioDeLotes.save(lote));
    }

    @Transactional(readOnly = true)
    public TicketBatchResponse buscarPorId(UUID loteId) {
        return repositorioDeLotes.findById(loteId)
                .map(TicketBatchResponse::de)
                .orElseThrow(() -> new TicketBatchNotFoundException(loteId));
    }

    @Transactional(readOnly = true)
    public TicketBatchPageResponse listarPorEvento(UUID eventoId, int pagina, int tamanho) {
        if (!repositorioDeEventos.existsById(eventoId)) {
            throw new EventNotFoundException(eventoId);
        }

        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "nome"));
        Page<TicketBatchResponse> lotes = repositorioDeLotes
                .buscarPorEventoId(eventoId, paginacao)
                .map(TicketBatchResponse::de);

        return TicketBatchPageResponse.de(lotes);
    }
}
