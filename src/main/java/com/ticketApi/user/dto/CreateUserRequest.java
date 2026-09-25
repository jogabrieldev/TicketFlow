package com.ticketApi.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "O nome do usuário é obrigatório")
        @Size(max = 255, message = "O nome do usuário deve ter no máximo 255 caracteres")
        String nome,

        @NotBlank(message = "O e-mail do usuário é obrigatório")
        @Email(message = "O e-mail do usuário possui formato inválido")
        @Size(max = 320, message = "O e-mail do usuário deve ter no máximo 320 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha
) {
}
