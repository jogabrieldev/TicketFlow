package com.ticketApi.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapeadorDeObjetos;

    public RestAuthenticationEntryPoint(ObjectMapper mapeadorDeObjetos) {
        this.mapeadorDeObjetos = mapeadorDeObjetos;
    }

    @Override
    public void commence(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            AuthenticationException excecao
    ) throws IOException, ServletException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "É necessário informar credenciais válidas para acessar este recurso"
        );
        problema.setTitle("Autenticação necessária");
        problema.setInstance(URI.create(requisicao.getRequestURI()));

        resposta.setStatus(HttpStatus.UNAUTHORIZED.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapeadorDeObjetos.writeValue(resposta.getOutputStream(), problema);
    }
}
