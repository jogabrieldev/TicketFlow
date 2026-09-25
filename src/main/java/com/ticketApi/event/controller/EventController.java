package com.ticketApi.event.controller;

import com.ticketApi.event.dto.CreateEventRequest;
import com.ticketApi.event.dto.EventPageResponse;
import com.ticketApi.event.dto.EventResponse;
import com.ticketApi.event.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService servicoDeEventos;

    public EventController(EventService servicoDeEventos) {
        this.servicoDeEventos = servicoDeEventos;
    }

    @PostMapping
    public ResponseEntity<EventResponse> criar(@Valid @RequestBody CreateEventRequest requisicao) {
        EventResponse evento = servicoDeEventos.criar(requisicao);

        return ResponseEntity.created(URI.create("/api/events/" + evento.id())).body(evento);
    }

    @GetMapping("/{eventoId}")
    public EventResponse buscarPorId(@PathVariable UUID eventoId) {
        return servicoDeEventos.buscarPorId(eventoId);
    }

    @GetMapping
    public EventPageResponse listar(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "A página não pode ser negativa") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho da página deve ser de pelo menos 1")
            @Max(value = 100, message = "O tamanho da página deve ser de no máximo 100") int tamanho
    ) {
        return servicoDeEventos.listar(pagina, tamanho);
    }
}
