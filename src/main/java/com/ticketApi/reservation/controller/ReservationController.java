package com.ticketApi.reservation.controller;

import com.ticketApi.reservation.dto.CreateReservationRequest;
import com.ticketApi.reservation.dto.ReservationPageResponse;
import com.ticketApi.reservation.dto.ReservationResponse;
import com.ticketApi.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/reservations")
@SecurityRequirement(name = "autenticacaoBasica")
public class ReservationController {

    private final ReservationService servicoDeReservas;

    public ReservationController(ReservationService servicoDeReservas) {
        this.servicoDeReservas = servicoDeReservas;
    }

    @PostMapping
    @Operation(summary = "Criar reserva", description = "Cria uma reserva para o cliente autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva criada"),
            @ApiResponse(responseCode = "400", description = "Dados ou composição da reserva inválidos"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão"),
            @ApiResponse(responseCode = "404", description = "Usuário ou lote não encontrado"),
            @ApiResponse(responseCode = "409", description = "Quantidade de ingressos indisponível")
    })
    public ResponseEntity<ReservationResponse> criar(
            Authentication autenticacao,
            @Valid @RequestBody CreateReservationRequest requisicao
    ) {
        ReservationResponse reserva = servicoDeReservas.criar(autenticacao.getName(), requisicao);

        return ResponseEntity
                .created(URI.create("/api/reservations/" + reserva.id()))
                .body(reserva);
    }

    @GetMapping("/me")
    @Operation(summary = "Listar minhas reservas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservas listadas"),
            @ApiResponse(responseCode = "400", description = "Paginação inválida"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    })
    public ReservationPageResponse listarMinhasReservas(
            Authentication autenticacao,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "A página não pode ser negativa") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho da página deve ser de pelo menos 1")
            @Max(value = 100, message = "O tamanho da página deve ser de no máximo 100") int tamanho
    ) {
        return servicoDeReservas.listarDoUsuario(autenticacao.getName(), pagina, tamanho);
    }
}
