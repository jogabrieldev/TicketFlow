package com.ticketApi.user.controller;

import com.ticketApi.auth.config.SecurityConfig;
import com.ticketApi.shared.exception.GlobalExceptionHandler;
import com.ticketApi.user.dto.UserResponse;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.exception.EmailAlreadyRegisteredException;
import com.ticketApi.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class UserControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("d6d15fd8-5001-4cc4-a399-1b3e183c6557");

    @Autowired
    private MockMvc simuladorMvc;

    @MockitoBean
    private UserService servicoDeUsuarios;

    @Test
    void devePermitirCadastroSemAutenticacaoESemCsrf() throws Exception {
        given(servicoDeUsuarios.cadastrar(any())).willReturn(criarRespostaDeUsuario());

        simuladorMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Maria Silva",
                                  "email": "maria@exemplo.com",
                                  "senha": "senha-segura"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/" + USUARIO_ID))
                .andExpect(jsonPath("$.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("CLIENTE"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void deveRejeitarDadosDeCadastroInvalidos() throws Exception {
        simuladorMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    void deveRetornarConflitoParaEmailJaCadastrado() throws Exception {
        given(servicoDeUsuarios.cadastrar(any()))
                .willThrow(new EmailAlreadyRegisteredException("maria@exemplo.com"));

        simuladorMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Maria Silva",
                                  "email": "maria@exemplo.com",
                                  "senha": "senha-segura"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("E-mail já cadastrado"))
                .andExpect(jsonPath("$.detail")
                        .value("Já existe um usuário cadastrado com o e-mail: maria@exemplo.com"));
    }

    private UserResponse criarRespostaDeUsuario() {
        OffsetDateTime criadoEm = OffsetDateTime.parse("2026-09-25T10:00:00-03:00");
        return new UserResponse(
                USUARIO_ID,
                "Maria Silva",
                "maria@exemplo.com",
                UserRole.CLIENTE,
                criadoEm,
                criadoEm
        );
    }
}
