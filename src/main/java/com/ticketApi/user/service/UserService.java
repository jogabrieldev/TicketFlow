package com.ticketApi.user.service;

import com.ticketApi.user.dto.CreateUserRequest;
import com.ticketApi.user.dto.UserResponse;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.exception.EmailAlreadyRegisteredException;
import com.ticketApi.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UserService {

    private static final int LIMITE_BYTES_BCRYPT = 72;

    private final UserRepository repositorioDeUsuarios;
    private final PasswordEncoder codificadorDeSenha;

    public UserService(UserRepository repositorioDeUsuarios, PasswordEncoder codificadorDeSenha) {
        this.repositorioDeUsuarios = repositorioDeUsuarios;
        this.codificadorDeSenha = codificadorDeSenha;
    }

    @Transactional
    public UserResponse cadastrar(CreateUserRequest requisicao) {
        String emailNormalizado = normalizarEmail(requisicao.email());
        validarLimiteDaSenha(requisicao.senha());

        if (repositorioDeUsuarios.buscarPorEmail(emailNormalizado).isPresent()) {
            throw new EmailAlreadyRegisteredException(emailNormalizado);
        }

        String senhaHash = codificadorDeSenha.encode(requisicao.senha());
        User usuario = new User(requisicao.nome(), emailNormalizado,
                senhaHash,
                UserRole.CLIENTE
        );

        try {
            return UserResponse.de(repositorioDeUsuarios.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException excecao) {
            throw new EmailAlreadyRegisteredException(emailNormalizado);
        }
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validarLimiteDaSenha(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_BCRYPT) {
            throw new IllegalArgumentException("A senha excede o limite de 72 bytes permitido pelo BCrypt");
        }
    }
}
