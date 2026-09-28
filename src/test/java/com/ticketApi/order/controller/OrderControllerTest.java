package com.ticketApi.order.controller;

import com.ticketApi.auth.config.SecurityConfig;
import com.ticketApi.order.dto.OrderItemResponse;
import com.ticketApi.order.dto.OrderPageResponse;
import com.ticketApi.order.dto.OrderResponse;
import com.ticketApi.order.entity.OrderStatus;
import com.ticketApi.order.exception.OrderAlreadyExistsException;
import com.ticketApi.order.service.OrderService;
import com.ticketApi.shared.exception.GlobalExceptionHandler;
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

@WebMvcTest(OrderController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class OrderControllerTest {

    private static final String EMAIL = "cliente@ticketflow.com";
    private static final UUID PEDIDO_ID = UUID.fromString("7705ba6a-aa10-4f89-a196-f4450a79d445");
    private static final UUID RESERVA_ID = UUID.fromString("52e655a8-c797-4d5c-9056-40b487638819");
    private static final UUID LOTE_ID = UUID.fromString("6ab8fe78-6b69-420a-85c2-167df0a0c5db");

    @Autowired
    private MockMvc simuladorMvc;

    @MockitoBean
    private OrderService servicoDePedidos;

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveCriarPedidoParaClienteAutenticado() throws Exception {
        given(servicoDePedidos.criar(eq(EMAIL), any())).willReturn(criarResposta());

        simuladorMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reservaId":"52e655a8-c797-4d5c-9056-40b487638819"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/" + PEDIDO_ID))
                .andExpect(jsonPath("$.status").value("PENDENTE_PAGAMENTO"))
                .andExpect(jsonPath("$.valorTotal").value(200.00));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveValidarIdentificadorDaReserva() throws Exception {
        simuladorMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("reservaId"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveRetornarConflitoParaPedidoDuplicado() throws Exception {
        given(servicoDePedidos.criar(eq(EMAIL), any())).willThrow(new OrderAlreadyExistsException(RESERVA_ID));

        simuladorMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reservaId":"52e655a8-c797-4d5c-9056-40b487638819"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Pedido não pode ser criado"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveListarPedidosDoCliente() throws Exception {
        given(servicoDePedidos.listarDoUsuario(EMAIL, 0, 20))
                .willReturn(new OrderPageResponse(List.of(criarResposta()), 0, 20, 1, 1));

        simuladorMvc.perform(get("/api/orders/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(PEDIDO_ID.toString()))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void deveExigirAutenticacao() throws Exception {
        simuladorMvc.perform(get("/api/orders/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveNegarAcessoAoAdministrador() throws Exception {
        simuladorMvc.perform(get("/api/orders/me"))
                .andExpect(status().isForbidden());
    }

    private static OrderResponse criarResposta() {
        OffsetDateTime agora = OffsetDateTime.parse("2030-01-01T12:00:00Z");
        OrderItemResponse item = new OrderItemResponse(
                LOTE_ID,
                "Primeiro lote",
                2,
                new BigDecimal("100.00"),
                new BigDecimal("200.00")
        );
        return new OrderResponse(
                PEDIDO_ID,
                RESERVA_ID,
                OrderStatus.PENDENTE_PAGAMENTO,
                new BigDecimal("200.00"),
                agora.plusMinutes(15),
                List.of(item),
                agora,
                agora
        );
    }
}
