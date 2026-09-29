package com.ticketApi.payment.controller;

import com.ticketApi.payment.dto.CreatePaymentRequest;
import com.ticketApi.payment.dto.PaymentResponse;
import com.ticketApi.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/payments")
@SecurityRequirement(name = "autenticacaoBasica")
public class PaymentController {

    private final PaymentService servicoDePagamentos;

    public PaymentController(PaymentService servicoDePagamentos) {
        this.servicoDePagamentos = servicoDePagamentos;
    }

    @PostMapping
    @Operation(
            summary = "Processar pagamento simulado",
            description = "Use tok_aprovado, tok_recusado ou tok_processando para simular o resultado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tentativa de pagamento criada"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "409", description = "Pedido indisponível para pagamento")
    })
    public ResponseEntity<PaymentResponse> criar(
            Authentication autenticacao,
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "A chave de idempotência é obrigatória")
            @Size(max = 255, message = "A chave de idempotência deve ter no máximo 255 caracteres")
            String chaveDeIdempotencia,
            @Valid @RequestBody CreatePaymentRequest requisicao
    ) {
        PaymentResponse pagamento = servicoDePagamentos.criar(autenticacao.getName(), chaveDeIdempotencia, requisicao);
        return ResponseEntity.created(URI.create("/api/payments/" + pagamento.id())).body(pagamento);
    }

    @GetMapping("/{pagamentoId}")
    @Operation(summary = "Consultar pagamento")
    public PaymentResponse buscar(
            Authentication autenticacao,
            @PathVariable UUID pagamentoId
    ) {
        return servicoDePagamentos.buscar(autenticacao.getName(), pagamentoId);
    }
}
