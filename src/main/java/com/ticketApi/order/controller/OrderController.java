package com.ticketApi.order.controller;

import com.ticketApi.order.dto.CreateOrderRequest;
import com.ticketApi.order.dto.OrderPageResponse;
import com.ticketApi.order.dto.OrderResponse;
import com.ticketApi.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "autenticacaoBasica")
public class OrderController {

    private final OrderService servicoDePedidos;

    public OrderController(OrderService servicoDePedidos) {
        this.servicoDePedidos = servicoDePedidos;
    }

    @PostMapping
    @Operation(summary = "Criar pedido", description = "Cria um pedido para uma reserva ativa do cliente autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão"),
            @ApiResponse(responseCode = "404", description = "Reserva não encontrada"),
            @ApiResponse(responseCode = "409", description = "Reserva indisponível ou pedido já existente")
    })
    public ResponseEntity<OrderResponse> criar(
            Authentication autenticacao,
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "A chave de idempotência é obrigatória")
            @Size(max = 255, message = "A chave de idempotência deve ter no máximo 255 caracteres")
            String chaveDeIdempotencia,
            @Valid @RequestBody CreateOrderRequest requisicao
    ) {
        OrderResponse pedido = servicoDePedidos.criar(
                autenticacao.getName(),
                chaveDeIdempotencia,
                requisicao
        );
        return ResponseEntity.created(URI.create("/api/orders/" + pedido.id())).body(pedido);
    }

    @GetMapping("/me")
    @Operation(summary = "Listar meus pedidos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos listados"),
            @ApiResponse(responseCode = "400", description = "Paginação inválida"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    })
    public OrderPageResponse listarMeusPedidos(
            Authentication autenticacao,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "A página não pode ser negativa") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho da página deve ser de pelo menos 1")
            @Max(value = 100, message = "O tamanho da página deve ser de no máximo 100") int tamanho
    ) {
        return servicoDePedidos.listarDoUsuario(autenticacao.getName(), pagina, tamanho);
    }
}
