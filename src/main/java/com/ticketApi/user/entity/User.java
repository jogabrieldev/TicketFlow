package com.ticketApi.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
public class User {

    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "O nome do usuário é obrigatório")
    @Size(max = 255, message = "O nome do usuário deve ter no máximo 255 caracteres")
    @Column(name = "name", nullable = false)
    private String nome;

    @NotBlank(message = "O e-mail do usuário é obrigatório")
    @Email(message = "O e-mail do usuário possui formato inválido")
    @Size(max = 320, message = "O e-mail do usuário deve ter no máximo 320 caracteres")
    @Column(nullable = false, length = 320)
    private String email;

    @NotBlank(message = "O hash da senha é obrigatório")
    @Size(max = 255, message = "O hash da senha deve ter no máximo 255 caracteres")
    @Column(name = "password_hash", nullable = false)
    private String senhaHash;

    @NotNull(message = "O papel do usuário é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole papel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected User() {
    }

    public User(String nome, String email, String senhaHash, UserRole papel) {
        validarNome(nome);
        validarEmail(email);
        validarSenhaHash(senhaHash);
        validarPapel(papel);

        this.id = UUID.randomUUID();
        this.nome = nome.trim();
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.papel = papel;
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do usuário é obrigatório");
        }
        if (nome.trim().length() > 255) {
            throw new IllegalArgumentException("O nome do usuário deve ter no máximo 255 caracteres");
        }
    }

    private static void validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail do usuário é obrigatório");
        }
        String emailNormalizado = normalizarEmail(email);
        if (emailNormalizado.length() > 320) {
            throw new IllegalArgumentException("O e-mail do usuário deve ter no máximo 320 caracteres");
        }
        if (!FORMATO_EMAIL.matcher(emailNormalizado).matches()) {
            throw new IllegalArgumentException("O e-mail do usuário possui formato inválido");
        }
    }

    private static void validarSenhaHash(String senhaHash) {
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new IllegalArgumentException("O hash da senha é obrigatório");
        }
        if (senhaHash.length() > 255) {
            throw new IllegalArgumentException("O hash da senha deve ter no máximo 255 caracteres");
        }
    }

    private static void validarPapel(UserRole papel) {
        if (papel == null) {
            throw new IllegalArgumentException("O papel do usuário é obrigatório");
        }
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public UUID obterId() {
        return id;
    }

    public String obterNome() {
        return nome;
    }

    public String obterEmail() {
        return email;
    }

    public String obterSenhaHash() {
        return senhaHash;
    }

    public UserRole obterPapel() {
        return papel;
    }

    public OffsetDateTime obterCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime obterAtualizadoEm() {
        return atualizadoEm;
    }
}
