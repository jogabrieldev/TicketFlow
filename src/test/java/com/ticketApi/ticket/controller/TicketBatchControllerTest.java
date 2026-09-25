package com.ticketApi.ticket.controller;

import com.ticketApi.auth.config.SecurityConfig;
import com.ticketApi.event.exception.EventNotFoundException;
import com.ticketApi.shared.exception.GlobalExceptionHandler;
import com.ticketApi.ticket.dto.TicketBatchPageResponse;
import com.ticketApi.ticket.dto.TicketBatchResponse;
import com.ticketApi.ticket.exception.TicketBatchNotFoundException;
import com.ticketApi.ticket.service.TicketBatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketBatchController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class TicketBatchControllerTest {

    private static final UUID EVENTO_ID = UUID.fromString("11f9ded0-ebea-4fdf-b2cf-671c0900ead5");
    private static final UUID LOTE_ID = UUID.fromString("791860d9-dead-4f73-8036-34213403ae7a");

    @Autowired
    private MockMvc simuladorMvc;

    @MockitoBean
    private TicketBatchService servicoDeLotes;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveCriarLote() throws Exception {
        given(servicoDeLotes.criar(eq(EVENTO_ID), any())).willReturn(criarRespostaDeLote());

        simuladorMvc.perform(post("/api/events/{eventoId}/ticket-batches", EVENTO_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Primeiro lote",
                                  "preco": 100.00,
                                  "quantidadeTotal": 500
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/ticket-batches/" + LOTE_ID))
                .andExpect(jsonPath("$.id").value(LOTE_ID.toString()))
                .andExpect(jsonPath("$.eventoId").value(EVENTO_ID.toString()))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(500));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveRejeitarRequisicaoDeCriacaoInvalida() throws Exception {
        simuladorMvc.perform(post("/api/events/{eventoId}/ticket-batches", EVENTO_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveRetornarNaoEncontradoAoCriarLoteParaEventoInexistente() throws Exception {
        given(servicoDeLotes.criar(eq(EVENTO_ID), any())).willThrow(new EventNotFoundException(EVENTO_ID));

        simuladorMvc.perform(post("/api/events/{eventoId}/ticket-batches", EVENTO_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Primeiro lote",
                                  "preco": 100.00,
                                  "quantidadeTotal": 500
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Evento não encontrado: " + EVENTO_ID));
    }

    @Test
    void deveBuscarLotePorId() throws Exception {
        given(servicoDeLotes.buscarPorId(LOTE_ID)).willReturn(criarRespostaDeLote());

        simuladorMvc.perform(get("/api/ticket-batches/{loteId}", LOTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LOTE_ID.toString()))
                .andExpect(jsonPath("$.nome").value("Primeiro lote"))
                .andExpect(jsonPath("$.preco").value(100.00));
    }

    @Test
    void deveRetornarNaoEncontradoQuandoLoteNaoExistir() throws Exception {
        given(servicoDeLotes.buscarPorId(LOTE_ID)).willThrow(new TicketBatchNotFoundException(LOTE_ID));

        simuladorMvc.perform(get("/api/ticket-batches/{loteId}", LOTE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Lote de ingressos não encontrado: " + LOTE_ID));
    }

    @Test
    void deveListarLotesDoEvento() throws Exception {
        TicketBatchPageResponse pagina = new TicketBatchPageResponse(
                List.of(criarRespostaDeLote()),
                0,
                20,
                1,
                1
        );
        given(servicoDeLotes.listarPorEvento(EVENTO_ID, 0, 20)).willReturn(pagina);

        simuladorMvc.perform(get("/api/events/{eventoId}/ticket-batches", EVENTO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(LOTE_ID.toString()))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    private TicketBatchResponse criarRespostaDeLote() {
        OffsetDateTime criadoEm = OffsetDateTime.parse("2026-09-25T10:00:00-03:00");
        return new TicketBatchResponse(
                LOTE_ID,
                EVENTO_ID,
                "Primeiro lote",
                new BigDecimal("100.00"),
                500,
                500,
                criadoEm,
                criadoEm
        );
    }
}
