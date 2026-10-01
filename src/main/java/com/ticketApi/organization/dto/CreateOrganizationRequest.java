package com.ticketApi.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank(message = "O nome fantasia da organização é obrigatório")
        @Size(max = 255, message = "O nome fantasia da organização deve ter no máximo 255 caracteres")
        String nomeFantasia,

        @NotBlank(message = "A razão social da organização é obrigatória")
        @Size(max = 255, message = "A razão social da organização deve ter no máximo 255 caracteres")
        String razaoSocial,

        @NotBlank(message = "O CNPJ da organização é obrigatório")
        @Size(max = 18, message = "O CNPJ da organização possui formato inválido")
        @Pattern(regexp = "[0-9./\\-]+", message = "O CNPJ da organização possui formato inválido")
        String cnpj,

        @NotBlank(message = "O e-mail da organização é obrigatório")
        @Email(message = "O e-mail da organização possui formato inválido")
        @Size(max = 320, message = "O e-mail da organização deve ter no máximo 320 caracteres")
        String email,

        @NotBlank(message = "O telefone da organização é obrigatório")
        @Size(max = 16, message = "O telefone da organização possui formato inválido")
        @Pattern(regexp = "[0-9()\\-\\s]+", message = "O telefone da organização possui formato inválido")
        String telefone,

        @NotBlank(message = "O logradouro da organização é obrigatório")
        @Size(max = 255, message = "O logradouro da organização deve ter no máximo 255 caracteres")
        String logradouro,

        @NotBlank(message = "O número do endereço da organização é obrigatório")
        @Size(max = 20, message = "O número do endereço da organização deve ter no máximo 20 caracteres")
        String numero,

        @Size(max = 255, message = "O complemento do endereço da organização deve ter no máximo 255 caracteres")
        String complemento,

        @NotBlank(message = "O bairro da organização é obrigatório")
        @Size(max = 100, message = "O bairro da organização deve ter no máximo 100 caracteres")
        String bairro,

        @NotBlank(message = "A cidade da organização é obrigatória")
        @Size(max = 100, message = "A cidade da organização deve ter no máximo 100 caracteres")
        String cidade,

        @NotBlank(message = "O estado da organização é obrigatório")
        @Pattern(regexp = "(?i)[A-Z]{2}", message = "O estado da organização deve possuir duas letras")
        String estado,

        @NotBlank(message = "O CEP da organização é obrigatório")
        @Pattern(regexp = "[0-9]{5}-?[0-9]{3}", message = "O CEP da organização possui formato inválido")
        String cep
) {
}
