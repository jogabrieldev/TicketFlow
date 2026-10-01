package com.ticketApi.organization.controller;

import com.ticketApi.organization.dto.CreateOrganizationRequest;
import com.ticketApi.organization.dto.OrganizationPageResponse;
import com.ticketApi.organization.dto.OrganizationResponse;
import com.ticketApi.organization.service.OrganizationService;
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
@RequestMapping("/api/organizations")
@SecurityRequirement(name = "autenticacaoBasica")
public class OrganizationController {

    private final OrganizationService servicoDeOrganizacoes;

    public OrganizationController(OrganizationService servicoDeOrganizacoes) {
        this.servicoDeOrganizacoes = servicoDeOrganizacoes;
    }

    @PostMapping
    @Operation(summary = "Criar organização", description = "Cria uma organização para o administrador autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organização criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão"),
            @ApiResponse(responseCode = "409", description = "CNPJ ou proprietário em conflito")
    })
    public ResponseEntity<OrganizationResponse> criar(
            Authentication autenticacao,
            @Valid @RequestBody CreateOrganizationRequest requisicao
    ) {
        OrganizationResponse organizacao = servicoDeOrganizacoes.criar(autenticacao.getName(), requisicao);
        return ResponseEntity.created(URI.create("/api/organizations/" + organizacao.id())).body(organizacao);
    }

    @GetMapping("/{organizacaoId}")
    @Operation(summary = "Buscar organização por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organização encontrada"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Usuário sem acesso à organização"),
            @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    })
    public OrganizationResponse buscarPorId(Authentication autenticacao, @PathVariable UUID organizacaoId) {
        return servicoDeOrganizacoes.buscarPorId(autenticacao.getName(), organizacaoId);
    }

    @GetMapping
    @Operation(summary = "Listar organizações acessíveis")
    public OrganizationPageResponse listar(
            Authentication autenticacao,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "A página não pode ser negativa") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho da página deve ser de pelo menos 1")
            @Max(value = 100, message = "O tamanho da página deve ser de no máximo 100") int tamanho
    ) {
        return servicoDeOrganizacoes.listar(autenticacao.getName(), pagina, tamanho);
    }
}
