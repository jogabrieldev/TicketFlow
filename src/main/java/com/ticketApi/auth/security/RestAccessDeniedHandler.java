package com.ticketApi.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapeadorDeObjetos;

    public RestAccessDeniedHandler(ObjectMapper mapeadorDeObjetos) {
        this.mapeadorDeObjetos = mapeadorDeObjetos;
    }

    @Override
    public void handle(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            AccessDeniedException excecao
    ) throws IOException, ServletException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "O usuário autenticado não possui permissão para acessar este recurso"
        );
        problema.setTitle("Acesso negado");
        problema.setInstance(URI.create(requisicao.getRequestURI()));

        resposta.setStatus(HttpStatus.FORBIDDEN.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapeadorDeObjetos.writeValue(resposta.getOutputStream(), problema);
    }
}
