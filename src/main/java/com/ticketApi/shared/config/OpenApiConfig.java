package com.ticketApi.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TicketFlow API",
                version = "v1",
                description = "API REST para gerenciamento e venda de ingressos de eventos"
        )
)
@SecurityScheme(
        name = "autenticacaoBasica",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Informe o e-mail e a senha de um usuário administrador"
)
public class OpenApiConfig {
}
