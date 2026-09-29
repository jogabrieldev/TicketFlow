package com.ticketApi.auth.config;

import com.ticketApi.auth.security.RestAccessDeniedHandler;
import com.ticketApi.auth.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    RestAuthenticationEntryPoint pontoDeEntradaDeAutenticacao(ObjectMapper mapeadorDeObjetos) {
        return new RestAuthenticationEntryPoint(mapeadorDeObjetos);
    }

    @Bean
    RestAccessDeniedHandler tratadorDeAcessoNegado(ObjectMapper mapeadorDeObjetos) {
        return new RestAccessDeniedHandler(mapeadorDeObjetos);
    }

    @Bean
    SecurityFilterChain filtroDeSeguranca(
            HttpSecurity http,
            RestAuthenticationEntryPoint pontoDeEntradaDeAutenticacao,
            RestAccessDeniedHandler tratadorDeAcessoNegado
    ) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/**", "/api/ticket-batches/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/reservations/**").hasRole("CLIENTE")
                        .requestMatchers("/api/orders/**").hasRole("CLIENTE")
                        .requestMatchers("/api/payments/**").hasRole("CLIENTE")
                        .anyRequest().denyAll()
                )
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(pontoDeEntradaDeAutenticacao)
                )
                .exceptionHandling(excecoes -> excecoes.authenticationEntryPoint(pontoDeEntradaDeAutenticacao)
                        .accessDeniedHandler(tratadorDeAcessoNegado)
                );

        return http.build();
    }
}
