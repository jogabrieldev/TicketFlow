package com.ticketApi.event.service;

import com.ticketApi.event.dto.CreateEventRequest;
import com.ticketApi.event.dto.EventPageResponse;
import com.ticketApi.event.dto.EventResponse;
import com.ticketApi.event.entity.Event;
import com.ticketApi.event.exception.EventNotFoundException;
import com.ticketApi.event.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EventService {

    private final EventRepository repositorioDeEventos;

    public EventService(EventRepository repositorioDeEventos) {
        this.repositorioDeEventos = repositorioDeEventos;
    }

    @Transactional
    public EventResponse criar(CreateEventRequest requisicao) {
        Event evento = new Event(
                requisicao.nome(),
                requisicao.descricao(),
                requisicao.local(),
                requisicao.inicioEm(),
                requisicao.terminoEm()
        );

        return EventResponse.de(repositorioDeEventos.save(evento));
    }

    @Transactional(readOnly = true)
    public EventResponse buscarPorId(UUID eventoId) {
        return repositorioDeEventos.findById(eventoId)
                .map(EventResponse::de)
                .orElseThrow(() -> new EventNotFoundException(eventoId));
    }

    @Transactional(readOnly = true)
    public EventPageResponse listar(int pagina, int tamanho) {
        PageRequest requisicaoDePagina = PageRequest.of(
                pagina,
                tamanho,
                Sort.by(Sort.Direction.ASC, "inicioEm")
        );
        Page<EventResponse> eventos = repositorioDeEventos.findAll(requisicaoDePagina).map(EventResponse::de);

        return EventPageResponse.de(eventos);
    }
}
