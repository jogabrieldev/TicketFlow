package com.ticketApi.auth.service;

import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UserRepository repositorioDeUsuarios;

    private DatabaseUserDetailsService servicoDeDetalhesDoUsuario;

    @BeforeEach
    void preparar() {
        servicoDeDetalhesDoUsuario = new DatabaseUserDetailsService(repositorioDeUsuarios);
    }

    @Test
    void deveCarregarClientePeloEmail() {
        User usuario = new User(
                "Maria Silva",
                "maria@exemplo.com",
                "hash-da-senha",
                UserRole.CLIENTE
        );
        given(repositorioDeUsuarios.buscarPorEmail("maria@exemplo.com")).willReturn(Optional.of(usuario));

        UserDetails detalhes = servicoDeDetalhesDoUsuario.loadUserByUsername("maria@exemplo.com");

        assertThat(detalhes.getUsername()).isEqualTo("maria@exemplo.com");
        assertThat(detalhes.getPassword()).isEqualTo("hash-da-senha");
        assertThat(detalhes.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CLIENTE");
    }

    @Test
    void deveCarregarAdministradorComAutoridadeCorreta() {
        User usuario = new User(
                "Administrador",
                "admin@exemplo.com",
                "hash-da-senha",
                UserRole.ADMINISTRADOR
        );
        given(repositorioDeUsuarios.buscarPorEmail("admin@exemplo.com")).willReturn(Optional.of(usuario));

        UserDetails detalhes = servicoDeDetalhesDoUsuario.loadUserByUsername("admin@exemplo.com");

        assertThat(detalhes.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    void deveOcultarSeEmailNaoExiste() {
        given(repositorioDeUsuarios.buscarPorEmail("ausente@exemplo.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicoDeDetalhesDoUsuario.loadUserByUsername("ausente@exemplo.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Credenciais inválidas");
    }
}
