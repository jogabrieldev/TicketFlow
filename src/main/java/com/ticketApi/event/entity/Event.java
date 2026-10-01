package com.ticketApi.event.entity;

import com.ticketApi.organization.entity.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "A organização do evento é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organizacao;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String nome;

    @Column(name = "description", columnDefinition = "TEXT")
    private String descricao;

    @NotBlank
    @Size(max = 255)
    @Column(name = "location", nullable = false)
    private String local;

    @NotNull
    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime inicioEm;

    @NotNull
    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime terminoEm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Event() {
    }

    public Event(
            Organization organizacao,
            String nome,
            String descricao,
            String local,
            OffsetDateTime inicioEm,
            OffsetDateTime terminoEm
    ) {
        if (organizacao == null) {
            throw new IllegalArgumentException("A organização do evento é obrigatória");
        }
        validarTextoObrigatorio(nome, "O nome do evento é obrigatório");
        validarTamanho(nome, "O nome do evento deve ter no máximo 255 caracteres");
        validarTextoObrigatorio(local, "O local do evento é obrigatório");
        validarTamanho(local, "O local do evento deve ter no máximo 255 caracteres");
        validarPeriodo(inicioEm, terminoEm);

        this.id = UUID.randomUUID();
        this.organizacao = organizacao;
        this.nome = nome.trim();
        this.descricao = normalizarTextoOpcional(descricao);
        this.local = local.trim();
        this.inicioEm = inicioEm;
        this.terminoEm = terminoEm;
    }

    private static void validarTextoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private static void validarTamanho(String valor, String mensagem) {
        if (valor.trim().length() > 255) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private static void validarPeriodo(OffsetDateTime inicioEm, OffsetDateTime terminoEm) {
        if (inicioEm == null) {
            throw new IllegalArgumentException("A data de início do evento é obrigatória");
        }
        if (terminoEm == null) {
            throw new IllegalArgumentException("A data de término do evento é obrigatória");
        }
        if (!terminoEm.isAfter(inicioEm)) {
            throw new IllegalArgumentException("A data de término do evento deve ser posterior à data de início");
        }
    }

    private static String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    public UUID obterId() {
        return id;
    }

    public Organization obterOrganizacao() {
        return organizacao;
    }

    public String obterNome() {
        return nome;
    }

    public String obterDescricao() {
        return descricao;
    }

    public String obterLocal() {
        return local;
    }

    public OffsetDateTime obterInicioEm() {
        return inicioEm;
    }

    public OffsetDateTime obterTerminoEm() {
        return terminoEm;
    }

    public OffsetDateTime obterCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime obterAtualizadoEm() {
        return atualizadoEm;
    }
}
