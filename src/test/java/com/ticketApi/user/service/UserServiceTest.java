package com.ticketApi.user.service;

import com.ticketApi.user.dto.CreateUserRequest;
import com.ticketApi.user.dto.UserResponse;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.exception.EmailAlreadyRegisteredException;
import com.ticketApi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repositorioDeUsuarios;

    @Mock
    private PasswordEncoder codificadorDeSenha;

    private UserService servicoDeUsuarios;

    @BeforeEach
    void preparar() {
        servicoDeUsuarios = new UserService(repositorioDeUsuarios, codificadorDeSenha);
    }

    @Test
    void deveCadastrarClienteComEmailNormalizadoESenhaCodificada() {
        CreateUserRequest requisicao = new CreateUserRequest(
                "Maria Silva",
                " Maria.Silva@Exemplo.com ",
                "senha-segura"
        );
        given(repositorioDeUsuarios.buscarPorEmail("maria.silva@exemplo.com")).willReturn(Optional.empty());
        given(codificadorDeSenha.encode("senha-segura")).willReturn("hash-da-senha");
        given(repositorioDeUsuarios.saveAndFlush(any(User.class)))
                .willAnswer(invocacao -> invocacao.getArgument(0));

        UserResponse resposta = servicoDeUsuarios.cadastrar(requisicao);

        ArgumentCaptor<User> capturador = ArgumentCaptor.forClass(User.class);
        verify(repositorioDeUsuarios).saveAndFlush(capturador.capture());
        User usuarioSalvo = capturador.getValue();
        assertThat(usuarioSalvo.obterEmail()).isEqualTo("maria.silva@exemplo.com");
        assertThat(usuarioSalvo.obterSenhaHash()).isEqualTo("hash-da-senha");
        assertThat(usuarioSalvo.obterPapel()).isEqualTo(UserRole.CLIENTE);
        assertThat(resposta.email()).isEqualTo("maria.silva@exemplo.com");
        assertThat(resposta.papel()).isEqualTo(UserRole.CLIENTE);
    }

    @Test
    void deveRejeitarEmailJaCadastrado() {
        CreateUserRequest requisicao = new CreateUserRequest(
                "Maria Silva",
                "maria@exemplo.com",
                "senha-segura"
        );
        User usuarioExistente = new User(
                "Outra Maria",
                "maria@exemplo.com",
                "hash-existente",
                UserRole.CLIENTE
        );
        given(repositorioDeUsuarios.buscarPorEmail("maria@exemplo.com"))
                .willReturn(Optional.of(usuarioExistente));

        assertThatThrownBy(() -> servicoDeUsuarios.cadastrar(requisicao))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Já existe um usuário cadastrado com o e-mail: maria@exemplo.com");
    }

    @Test
    void deveConverterConflitoDoBancoEmEmailJaCadastrado() {
        CreateUserRequest requisicao = new CreateUserRequest(
                "Maria Silva",
                "maria@exemplo.com",
                "senha-segura"
        );
        given(repositorioDeUsuarios.buscarPorEmail("maria@exemplo.com")).willReturn(Optional.empty());
        given(codificadorDeSenha.encode("senha-segura")).willReturn("hash-da-senha");
        given(repositorioDeUsuarios.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("restrição única"));

        assertThatThrownBy(() -> servicoDeUsuarios.cadastrar(requisicao))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Já existe um usuário cadastrado com o e-mail: maria@exemplo.com");
    }

    @Test
    void deveRejeitarSenhaQueExcedeLimiteDeBytesDoBCrypt() {
        String senhaComMuitosBytes = "á".repeat(40);
        CreateUserRequest requisicao = new CreateUserRequest(
                "Maria Silva",
                "maria@exemplo.com",
                senhaComMuitosBytes
        );

        assertThatThrownBy(() -> servicoDeUsuarios.cadastrar(requisicao))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A senha excede o limite de 72 bytes permitido pelo BCrypt");
    }
}
