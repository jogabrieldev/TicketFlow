package com.ticketApi.event.controller;

import com.ticketApi.auth.config.SecurityConfig;
import com.ticketApi.event.dto.EventPageResponse;
import com.ticketApi.event.dto.EventResponse;
import com.ticketApi.event.exception.EventNotFoundException;
import com.ticketApi.event.service.EventService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class EventControllerTest {

    private static final UUID EVENTO_ID = UUID.fromString("d5191ef8-5d9b-49c9-b5d2-241795a801fe");
    private static final OffsetDateTime INICIO_EM =
            OffsetDateTime.parse("2026-10-10T09:00:00-03:00");
    private static final OffsetDateTime TERMINO_EM =
            OffsetDateTime.parse("2026-10-10T18:00:00-03:00");

    @Autowired
    private MockMvc simuladorMvc;

    @MockitoBean
    private EventService servicoDeEventos;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveCriarEvento() throws Exception {
        given(servicoDeEventos.criar(any())).willReturn(criarRespostaDeEvento());

        simuladorMvc.perform(post("/api/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Java Conference",
                                  "descricao": "Java ecosystem",
                                  "local": "Convention Center",
                                  "inicioEm": "2026-10-10T09:00:00-03:00",
                                  "terminoEm": "2026-10-10T18:00:00-03:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/events/" + EVENTO_ID))
                .andExpect(jsonPath("$.id").value(EVENTO_ID.toString()))
                .andExpect(jsonPath("$.nome").value("Java Conference"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveRejeitarRequisicaoDeCriacaoInvalida() throws Exception {
        simuladorMvc.perform(post("/api/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    void deveBuscarEventoPorId() throws Exception {
        given(servicoDeEventos.buscarPorId(EVENTO_ID)).willReturn(criarRespostaDeEvento());

        simuladorMvc.perform(get("/api/events/{eventoId}", EVENTO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVENTO_ID.toString()))
                .andExpect(jsonPath("$.local").value("Convention Center"));
    }

    @Test
    void deveRetornarProblemaQuandoEventoNaoForEncontrado() throws Exception {
        given(servicoDeEventos.buscarPorId(EVENTO_ID)).willThrow(new EventNotFoundException(EVENTO_ID));

        simuladorMvc.perform(get("/api/events/{eventoId}", EVENTO_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Evento não encontrado: " + EVENTO_ID));
    }

    @Test
    void deveListarEventos() throws Exception {
        EventPageResponse pagina = new EventPageResponse(List.of(criarRespostaDeEvento()), 0, 20, 1, 1);
        given(servicoDeEventos.listar(eq(0), eq(20))).willReturn(pagina);

        simuladorMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(EVENTO_ID.toString()))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void deveRejeitarPaginacaoInvalida() throws Exception {
        simuladorMvc.perform(get("/api/events").param("pagina", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro de requisição inválido"))
                .andExpect(jsonPath("$.detail").value("A página não pode ser negativa"));
    }

    @Test
    void deveRejeitarIdentificadorInvalido() throws Exception {
        simuladorMvc.perform(get("/api/events/{eventoId}", "identificador-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro de requisição inválido"))
                .andExpect(jsonPath("$.detail").value("O parâmetro 'eventoId' possui um valor inválido"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveRejeitarCorpoComFormatoInvalido() throws Exception {
        simuladorMvc.perform(post("/api/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ formato inválido }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail")
                        .value("O corpo da requisição está ausente ou possui formato inválido"));
    }

    @Test
    void deveExigirAutenticacaoParaCriarEvento() throws Exception {
        simuladorMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Autenticação necessária"))
                .andExpect(jsonPath("$.detail")
                        .value("É necessário informar credenciais válidas para acessar este recurso"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void deveNegarCriacaoDeEventoParaCliente() throws Exception {
        simuladorMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.detail")
                        .value("O usuário autenticado não possui permissão para acessar este recurso"));
    }

    private EventResponse criarRespostaDeEvento() {
        return new EventResponse(
                EVENTO_ID,
                "Java Conference",
                "Java ecosystem",
                "Convention Center",
                INICIO_EM,
                TERMINO_EM,
                INICIO_EM.minusDays(1),
                INICIO_EM.minusDays(1)
        );
    }
}
