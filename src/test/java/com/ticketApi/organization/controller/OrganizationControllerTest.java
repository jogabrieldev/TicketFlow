package com.ticketApi.organization.controller;

import com.ticketApi.auth.config.SecurityConfig;
import com.ticketApi.organization.dto.OrganizationPageResponse;
import com.ticketApi.organization.dto.OrganizationResponse;
import com.ticketApi.organization.exception.CnpjAlreadyRegisteredException;
import com.ticketApi.organization.service.OrganizationService;
import com.ticketApi.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class OrganizationControllerTest {

    private static final UUID ORGANIZACAO_ID = UUID.fromString("6daf1528-4788-4b09-9ea8-b189350f4c1f");
    private static final String EMAIL_USUARIO = "maria@exemplo.com";

    @Autowired
    private MockMvc simuladorMvc;

    @MockitoBean
    private OrganizationService servicoDeOrganizacoes;

    @Test
    @WithMockUser(username = EMAIL_USUARIO, roles = "ADMINISTRADOR")
    void deveCriarOrganizacaoParaAdministrador() throws Exception {
        given(servicoDeOrganizacoes.criar(eq(EMAIL_USUARIO), any())).willReturn(criarResposta());

        simuladorMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/organizations/" + ORGANIZACAO_ID))
                .andExpect(jsonPath("$.id").value(ORGANIZACAO_ID.toString()))
                .andExpect(jsonPath("$.nomeFantasia").value("Ticket Flow"))
                .andExpect(jsonPath("$.cnpj").value("11222333000181"));
    }

    @Test
    void deveExigirAutenticacaoParaCriarOrganizacao() throws Exception {
        simuladorMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Autenticação necessária"));
    }

    @Test
    @WithMockUser(username = EMAIL_USUARIO, roles = "ADMINISTRADOR")
    void deveRejeitarCadastroInvalido() throws Exception {
        simuladorMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    @WithMockUser(username = EMAIL_USUARIO, roles = "ADMINISTRADOR")
    void deveRetornarConflitoParaCnpjJaCadastrado() throws Exception {
        given(servicoDeOrganizacoes.criar(eq(EMAIL_USUARIO), any()))
                .willThrow(new CnpjAlreadyRegisteredException("11222333000181"));

        simuladorMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Organização não pode ser criada"));
    }

    @Test
    @WithMockUser(username = EMAIL_USUARIO, roles = "ADMINISTRADOR")
    void deveBuscarOrganizacaoAcessivel() throws Exception {
        given(servicoDeOrganizacoes.buscarPorId(EMAIL_USUARIO, ORGANIZACAO_ID)).willReturn(criarResposta());

        simuladorMvc.perform(get("/api/organizations/{organizacaoId}", ORGANIZACAO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Ticket Flow Tecnologia Ltda"))
                .andExpect(jsonPath("$.email").value("contato@ticketflow.com.br"));
    }

    @Test
    @WithMockUser(username = EMAIL_USUARIO, roles = "CLIENTE")
    void deveNegarConsultaDeOrganizacaoParaCliente() throws Exception {
        simuladorMvc.perform(get("/api/organizations/{organizacaoId}", ORGANIZACAO_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    @WithMockUser(username = EMAIL_USUARIO, roles = "CLIENTE")
    void deveNegarCriacaoDeOrganizacaoParaCliente() throws Exception {
        simuladorMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    @WithMockUser(username = EMAIL_USUARIO, roles = "ADMINISTRADOR")
    void deveListarOrganizacoesParaAdministrador() throws Exception {
        OrganizationPageResponse pagina = new OrganizationPageResponse(
                List.of(criarResposta()), 0, 20, 1, 1
        );
        given(servicoDeOrganizacoes.listar(EMAIL_USUARIO, 0, 20)).willReturn(pagina);

        simuladorMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(ORGANIZACAO_ID.toString()))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    private String requisicaoValida() {
        return """
                {
                  "nomeFantasia": "Ticket Flow",
                  "razaoSocial": "Ticket Flow Tecnologia Ltda",
                  "cnpj": "11.222.333/0001-81",
                  "email": "contato@ticketflow.com.br",
                  "telefone": "(11) 98765-4321",
                  "logradouro": "Avenida Paulista",
                  "numero": "1000",
                  "complemento": "10º andar",
                  "bairro": "Bela Vista",
                  "cidade": "São Paulo",
                  "estado": "SP",
                  "cep": "01310-100"
                }
                """;
    }

    private OrganizationResponse criarResposta() {
        OffsetDateTime agora = OffsetDateTime.parse("2026-09-30T10:00:00-03:00");
        return new OrganizationResponse(
                ORGANIZACAO_ID,
                "Ticket Flow",
                "Ticket Flow Tecnologia Ltda",
                "11222333000181",
                "contato@ticketflow.com.br",
                "11987654321",
                "Avenida Paulista",
                "1000",
                "10º andar",
                "Bela Vista",
                "São Paulo",
                "SP",
                "01310100",
                agora,
                agora
        );
    }
}
