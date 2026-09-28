package com.ticketApi.reservation.controller;

import com.ticketApi.auth.config.SecurityConfig;
import com.ticketApi.reservation.dto.ReservationItemResponse;
import com.ticketApi.reservation.dto.ReservationPageResponse;
import com.ticketApi.reservation.dto.ReservationResponse;
import com.ticketApi.reservation.entity.ReservationStatus;
import com.ticketApi.reservation.service.ReservationService;
import com.ticketApi.shared.exception.GlobalExceptionHandler;
import com.ticketApi.ticket.exception.InsufficientTicketAvailabilityException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ReservationControllerTest {

    private static final String EMAIL = "maria@exemplo.com";
    private static final UUID RESERVA_ID = UUID.fromString("90a9e01a-f935-4c67-a911-79add48bec38");
    private static final UUID EVENTO_ID = UUID.fromString("0da09ae8-e43f-45a0-b047-54136b76c738");
    private static final UUID LOTE_ID = UUID.fromString("8d0c4bbc-6bf5-4e50-81ba-2a6f0ca302e7");

    @Autowired
    private MockMvc simuladorMvc;

    @MockitoBean
    private ReservationService servicoDeReservas;

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveCriarReservaParaClienteAutenticado() throws Exception {
        given(servicoDeReservas.criar(eq(EMAIL), any())).willReturn(criarResposta());

        simuladorMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {
                                      "loteId": "8d0c4bbc-6bf5-4e50-81ba-2a6f0ca302e7",
                                      "quantidade": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reservations/" + RESERVA_ID))
                .andExpect(jsonPath("$.id").value(RESERVA_ID.toString()))
                .andExpect(jsonPath("$.eventoId").value(EVENTO_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.itens[0].loteId").value(LOTE_ID.toString()))
                .andExpect(jsonPath("$.itens[0].subtotal").value(200.00))
                .andExpect(jsonPath("$.valorTotal").value(200.00));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveRejeitarReservaSemItens() throws Exception {
        simuladorMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.erros[0].campo").value("itens"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveRetornarConflitoQuandoNaoHouverDisponibilidade() throws Exception {
        given(servicoDeReservas.criar(eq(EMAIL), any()))
                .willThrow(new InsufficientTicketAvailabilityException(LOTE_ID, 3));

        simuladorMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {
                                      "loteId": "8d0c4bbc-6bf5-4e50-81ba-2a6f0ca302e7",
                                      "quantidade": 3
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Ingressos indisponíveis"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveListarReservasDoClienteAutenticado() throws Exception {
        ReservationPageResponse pagina = new ReservationPageResponse(List.of(criarResposta()), 0, 20, 1, 1);
        given(servicoDeReservas.listarDoUsuario(EMAIL, 0, 20)).willReturn(pagina);

        simuladorMvc.perform(get("/api/reservations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(RESERVA_ID.toString()))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void deveExigirAutenticacaoParaCriarReserva() throws Exception {
        simuladorMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Autenticação necessária"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveNegarCriacaoDeReservaParaAdministrador() throws Exception {
        simuladorMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    private static ReservationResponse criarResposta() {
        OffsetDateTime criadoEm = OffsetDateTime.parse("2026-09-28T11:00:00-03:00");
        ReservationItemResponse item = new ReservationItemResponse(
                UUID.fromString("b310ca4d-40bb-44df-a8a8-b6a3b67f5485"),
                LOTE_ID,
                "Primeiro lote",
                2,
                new BigDecimal("100.00"),
                new BigDecimal("200.00")
        );
        return new ReservationResponse(
                RESERVA_ID,
                EVENTO_ID,
                ReservationStatus.PENDENTE,
                criadoEm.plusMinutes(15),
                List.of(item),
                new BigDecimal("200.00"),
                criadoEm,
                criadoEm
        );
    }
}
