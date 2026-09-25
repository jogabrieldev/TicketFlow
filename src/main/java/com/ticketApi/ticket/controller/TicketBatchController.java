package com.ticketApi.ticket.controller;

import com.ticketApi.ticket.dto.CreateTicketBatchRequest;
import com.ticketApi.ticket.dto.TicketBatchPageResponse;
import com.ticketApi.ticket.dto.TicketBatchResponse;
import com.ticketApi.ticket.service.TicketBatchService;
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
@RequestMapping("/api")
public class TicketBatchController {

    private final TicketBatchService servicoDeLotes;

    public TicketBatchController(TicketBatchService servicoDeLotes) {
        this.servicoDeLotes = servicoDeLotes;
    }

    @PostMapping("/events/{eventoId}/ticket-batches")
    public ResponseEntity<TicketBatchResponse> criar(
            @PathVariable UUID eventoId,
            @Valid @RequestBody CreateTicketBatchRequest requisicao
    ) {
        TicketBatchResponse lote = servicoDeLotes.criar(eventoId, requisicao);

        return ResponseEntity
                .created(URI.create("/api/ticket-batches/" + lote.id()))
                .body(lote);
    }

    @GetMapping("/events/{eventoId}/ticket-batches")
    public TicketBatchPageResponse listarPorEvento(
            @PathVariable UUID eventoId,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "A página não pode ser negativa") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho da página deve ser de pelo menos 1")
            @Max(value = 100, message = "O tamanho da página deve ser de no máximo 100") int tamanho
    ) {
        return servicoDeLotes.listarPorEvento(eventoId, pagina, tamanho);
    }

    @GetMapping("/ticket-batches/{loteId}")
    public TicketBatchResponse buscarPorId(@PathVariable UUID loteId) {
        return servicoDeLotes.buscarPorId(loteId);
    }
}
