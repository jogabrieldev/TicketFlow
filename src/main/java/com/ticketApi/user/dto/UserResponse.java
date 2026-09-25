package com.ticketApi.user.dto;

import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String nome,
        String email,
        UserRole papel,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {

    public static UserResponse de(User usuario) {
        return new UserResponse(
                usuario.obterId(),
                usuario.obterNome(),
                usuario.obterEmail(),
                usuario.obterPapel(),
                usuario.obterCriadoEm(),
                usuario.obterAtualizadoEm()
        );
    }
}
