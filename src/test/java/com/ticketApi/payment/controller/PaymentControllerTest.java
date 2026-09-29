package com.ticketApi.payment.controller;

import com.ticketApi.auth.config.SecurityConfig;
import com.ticketApi.payment.dto.PaymentResponse;
import com.ticketApi.payment.entity.PaymentStatus;
import com.ticketApi.payment.exception.PaymentUnavailableException;
import com.ticketApi.payment.service.PaymentService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class PaymentControllerTest {

    private static final String EMAIL = "cliente@ticketflow.com";
    private static final String CHAVE = "pagamento-001";
    private static final UUID PAGAMENTO_ID = UUID.fromString("9d942d96-1007-420d-97fa-eebfe157171b");
    private static final UUID PEDIDO_ID = UUID.fromString("7705ba6a-aa10-4f89-a196-f4450a79d445");

    @Autowired
    private MockMvc simuladorMvc;

    @MockitoBean
    private PaymentService servicoDePagamentos;

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveProcessarPagamento() throws Exception {
        given(servicoDePagamentos.criar(eq(EMAIL), eq(CHAVE), any())).willReturn(resposta());

        simuladorMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", CHAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":"7705ba6a-aa10-4f89-a196-f4450a79d445","tokenPagamento":"tok_aprovado"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/payments/" + PAGAMENTO_ID))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.valor").value(200.00));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveValidarCorpo() throws Exception {
        simuladorMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", CHAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.length()").value(2));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveRetornarConflitoQuandoPedidoEstiverIndisponivel() throws Exception {
        given(servicoDePagamentos.criar(eq(EMAIL), eq(CHAVE), any()))
                .willThrow(new PaymentUnavailableException(PEDIDO_ID));

        simuladorMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", CHAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":"7705ba6a-aa10-4f89-a196-f4450a79d445","tokenPagamento":"tok_aprovado"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Pagamento não pode ser processado"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CLIENTE")
    void deveConsultarPagamento() throws Exception {
        given(servicoDePagamentos.buscar(EMAIL, PAGAMENTO_ID)).willReturn(resposta());

        simuladorMvc.perform(get("/api/payments/{id}", PAGAMENTO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAGAMENTO_ID.toString()));
    }

    @Test
    void deveExigirAutenticacao() throws Exception {
        simuladorMvc.perform(get("/api/payments/{id}", PAGAMENTO_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveNegarAcessoAoAdministrador() throws Exception {
        simuladorMvc.perform(get("/api/payments/{id}", PAGAMENTO_ID))
                .andExpect(status().isForbidden());
    }

    private static PaymentResponse resposta() {
        OffsetDateTime agora = OffsetDateTime.parse("2030-01-01T12:00:00Z");
        return new PaymentResponse(
                PAGAMENTO_ID,
                PEDIDO_ID,
                PaymentStatus.APROVADO,
                new BigDecimal("200.00"),
                "fake_referencia",
                agora,
                agora
        );
    }
}
