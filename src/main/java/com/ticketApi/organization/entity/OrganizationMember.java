package com.ticketApi.organization.entity;

import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_members")
public class OrganizationMember {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "A organização do membro é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organizacao;

    @NotNull(message = "O usuário do membro é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User usuario;

    @NotNull(message = "O papel do membro é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private OrganizationMemberRole papel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected OrganizationMember() {
    }

    public OrganizationMember(Organization organizacao, User usuario, OrganizationMemberRole papel) {
        if (organizacao == null) {
            throw new IllegalArgumentException("A organização do membro é obrigatória");
        }
        if (usuario == null) {
            throw new IllegalArgumentException("O usuário do membro é obrigatório");
        }
        if (usuario.obterPapel() != UserRole.ADMINISTRADOR) {
            throw new IllegalArgumentException("Somente administradores podem pertencer a uma organização");
        }
        if (papel == null) {
            throw new IllegalArgumentException("O papel do membro é obrigatório");
        }

        this.id = UUID.randomUUID();
        this.organizacao = organizacao;
        this.usuario = usuario;
        this.papel = papel;
    }

    public UUID obterId() {
        return id;
    }

    public Organization obterOrganizacao() {
        return organizacao;
    }

    public User obterUsuario() {
        return usuario;
    }

    public OrganizationMemberRole obterPapel() {
        return papel;
    }

    public OffsetDateTime obterCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime obterAtualizadoEm() {
        return atualizadoEm;
    }
}
