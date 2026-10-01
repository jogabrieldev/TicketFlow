package com.ticketApi.event.controller;

import com.ticketApi.event.dto.CreateEventRequest;
import com.ticketApi.event.dto.EventPageResponse;
import com.ticketApi.event.dto.EventResponse;
import com.ticketApi.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api")
public class EventController {

    private final EventService servicoDeEventos;

    public EventController(EventService servicoDeEventos) {
        this.servicoDeEventos = servicoDeEventos;
    }

    @PostMapping("/organizations/{organizacaoId}/events")
    @Operation(
            summary = "Criar evento",
            description = "Cria um evento para uma organização. Operação restrita a administradores.",
            security = @SecurityRequirement(name = "autenticacaoBasica")
    )
    public ResponseEntity<EventResponse> criar(
            Authentication autenticacao,
            @PathVariable UUID organizacaoId,
            @Valid @RequestBody CreateEventRequest requisicao
    ) {
        EventResponse evento = servicoDeEventos.criar(autenticacao.getName(), organizacaoId, requisicao);

        return ResponseEntity.created(URI.create("/api/events/" + evento.id())).body(evento);
    }

    @GetMapping("/events/{eventoId}")
    public EventResponse buscarPorId(@PathVariable UUID eventoId) {
        return servicoDeEventos.buscarPorId(eventoId);
    }

    @GetMapping("/events")
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
