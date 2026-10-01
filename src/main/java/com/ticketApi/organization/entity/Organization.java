package com.ticketApi.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "O nome fantasia da organização é obrigatório")
    @Size(max = 255, message = "O nome fantasia da organização deve ter no máximo 255 caracteres")
    @Column(name = "trade_name", nullable = false)
    private String nomeFantasia;

    @NotBlank(message = "A razão social da organização é obrigatória")
    @Size(max = 255, message = "A razão social da organização deve ter no máximo 255 caracteres")
    @Column(name = "legal_name", nullable = false)
    private String razaoSocial;

    @NotBlank(message = "O CNPJ da organização é obrigatório")
    @Size(min = 14, max = 14, message = "O CNPJ da organização deve possuir 14 dígitos")
    @Column(nullable = false, length = 14)
    private String cnpj;

    @NotBlank(message = "O e-mail da organização é obrigatório")
    @Email(message = "O e-mail da organização possui formato inválido")
    @Size(max = 320, message = "O e-mail da organização deve ter no máximo 320 caracteres")
    @Column(nullable = false, length = 320)
    private String email;

    @NotBlank(message = "O telefone da organização é obrigatório")
    @Size(min = 10, max = 11, message = "O telefone da organização deve possuir 10 ou 11 dígitos")
    @Column(name = "phone", nullable = false, length = 11)
    private String telefone;

    @NotBlank(message = "O logradouro da organização é obrigatório")
    @Size(max = 255, message = "O logradouro da organização deve ter no máximo 255 caracteres")
    @Column(name = "street", nullable = false)
    private String logradouro;

    @NotBlank(message = "O número do endereço da organização é obrigatório")
    @Size(max = 20, message = "O número do endereço da organização deve ter no máximo 20 caracteres")
    @Column(name = "address_number", nullable = false, length = 20)
    private String numero;

    @Size(max = 255, message = "O complemento do endereço da organização deve ter no máximo 255 caracteres")
    @Column(name = "address_complement")
    private String complemento;

    @NotBlank(message = "O bairro da organização é obrigatório")
    @Size(max = 100, message = "O bairro da organização deve ter no máximo 100 caracteres")
    @Column(name = "neighborhood", nullable = false, length = 100)
    private String bairro;

    @NotBlank(message = "A cidade da organização é obrigatória")
    @Size(max = 100, message = "A cidade da organização deve ter no máximo 100 caracteres")
    @Column(name = "city", nullable = false, length = 100)
    private String cidade;

    @NotBlank(message = "O estado da organização é obrigatório")
    @Size(min = 2, max = 2, message = "O estado da organização deve possuir duas letras")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "state", nullable = false, length = 2, columnDefinition = "CHAR(2)")
    private String estado;

    @NotBlank(message = "O CEP da organização é obrigatório")
    @Size(min = 8, max = 8, message = "O CEP da organização deve possuir 8 dígitos")
    @Column(name = "postal_code", nullable = false, length = 8)
    private String cep;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Organization() {
    }

    public Organization(
            String nomeFantasia,
            String razaoSocial,
            String cnpj,
            String email,
            String telefone,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String estado,
            String cep
    ) {
        validarTextoObrigatorio(nomeFantasia, 255, "O nome fantasia da organização é obrigatório",
                "O nome fantasia da organização deve ter no máximo 255 caracteres");
        validarTextoObrigatorio(razaoSocial, 255, "A razão social da organização é obrigatória",
                "A razão social da organização deve ter no máximo 255 caracteres");
        validarCnpj(cnpj);
        validarEmail(email);
        validarTelefone(telefone);
        validarTextoObrigatorio(logradouro, 255, "O logradouro da organização é obrigatório",
                "O logradouro da organização deve ter no máximo 255 caracteres");
        validarTextoObrigatorio(numero, 20, "O número do endereço da organização é obrigatório",
                "O número do endereço da organização deve ter no máximo 20 caracteres");
        validarTextoOpcional(complemento, 255,
                "O complemento do endereço da organização deve ter no máximo 255 caracteres");
        validarTextoObrigatorio(bairro, 100, "O bairro da organização é obrigatório",
                "O bairro da organização deve ter no máximo 100 caracteres");
        validarTextoObrigatorio(cidade, 100, "A cidade da organização é obrigatória",
                "A cidade da organização deve ter no máximo 100 caracteres");
        validarEstado(estado);
        validarCep(cep);

        this.id = UUID.randomUUID();
        this.nomeFantasia = nomeFantasia.trim();
        this.razaoSocial = razaoSocial.trim();
        this.cnpj = somenteDigitos(cnpj);
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.telefone = somenteDigitos(telefone);
        this.logradouro = logradouro.trim();
        this.numero = numero.trim();
        this.complemento = normalizarTextoOpcional(complemento);
        this.bairro = bairro.trim();
        this.cidade = cidade.trim();
        this.estado = estado.trim().toUpperCase(Locale.ROOT);
        this.cep = somenteDigitos(cep);
    }

    private static void validarTextoObrigatorio(
            String valor,
            int tamanhoMaximo,
            String mensagemObrigatorio,
            String mensagemTamanho
    ) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagemObrigatorio);
        }
        if (valor.trim().length() > tamanhoMaximo) {
            throw new IllegalArgumentException(mensagemTamanho);
        }
    }

    private static void validarTextoOpcional(String valor, int tamanhoMaximo, String mensagemTamanho) {
        if (valor != null && valor.trim().length() > tamanhoMaximo) {
            throw new IllegalArgumentException(mensagemTamanho);
        }
    }

    private static void validarCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            throw new IllegalArgumentException("O CNPJ da organização é obrigatório");
        }
        String valor = somenteDigitos(cnpj);
        if (valor.length() != 14 || valor.chars().distinct().count() == 1
                || calcularDigitoCnpj(valor, 12) != Character.digit(valor.charAt(12), 10)
                || calcularDigitoCnpj(valor, 13) != Character.digit(valor.charAt(13), 10)) {
            throw new IllegalArgumentException("O CNPJ da organização é inválido");
        }
    }

    private static int calcularDigitoCnpj(String cnpj, int quantidadeDeDigitos) {
        int[] pesos = quantidadeDeDigitos == 12 ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int indice = 0; indice < quantidadeDeDigitos; indice++) {
            soma += Character.digit(cnpj.charAt(indice), 10) * pesos[indice];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static void validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail da organização é obrigatório");
        }
        String valor = email.trim();
        if (valor.length() > 320 || !valor.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("O e-mail da organização possui formato inválido");
        }
    }

    private static void validarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new IllegalArgumentException("O telefone da organização é obrigatório");
        }
        if (!somenteDigitos(telefone).matches("\\d{10,11}")) {
            throw new IllegalArgumentException("O telefone da organização deve possuir 10 ou 11 dígitos");
        }
    }

    private static void validarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("O estado da organização é obrigatório");
        }
        if (!estado.trim().matches("(?i)[A-Z]{2}")) {
            throw new IllegalArgumentException("O estado da organização deve possuir duas letras");
        }
    }

    private static void validarCep(String cep) {
        if (cep == null || cep.isBlank()) {
            throw new IllegalArgumentException("O CEP da organização é obrigatório");
        }
        if (!somenteDigitos(cep).matches("\\d{8}")) {
            throw new IllegalArgumentException("O CEP da organização deve possuir 8 dígitos");
        }
    }

    private static String somenteDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }

    private static String normalizarTextoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public UUID obterId() {
        return id;
    }

    public String obterNomeFantasia() {
        return nomeFantasia;
    }

    public String obterRazaoSocial() {
        return razaoSocial;
    }

    public String obterCnpj() {
        return cnpj;
    }

    public String obterEmail() {
        return email;
    }

    public String obterTelefone() {
        return telefone;
    }

    public String obterLogradouro() {
        return logradouro;
    }

    public String obterNumero() {
        return numero;
    }

    public String obterComplemento() {
        return complemento;
    }

    public String obterBairro() {
        return bairro;
    }

    public String obterCidade() {
        return cidade;
    }

    public String obterEstado() {
        return estado;
    }

    public String obterCep() {
        return cep;
    }

    public OffsetDateTime obterCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime obterAtualizadoEm() {
        return atualizadoEm;
    }
}
