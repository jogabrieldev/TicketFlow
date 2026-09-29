package com.ticketApi.reservation.entity;

import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "O usuário da reserva é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User usuario;

    @NotNull(message = "O status da reserva é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @NotNull(message = "A data de expiração da reserva é obrigatória")
    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiraEm;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationItem> itens = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Reservation() {
    }

    public Reservation(User usuario, OffsetDateTime expiraEm, OffsetDateTime agora) {
        validarUsuario(usuario);
        validarExpiracao(expiraEm, agora);

        this.id = UUID.randomUUID();
        this.usuario = usuario;
        this.status = ReservationStatus.PENDENTE;
        this.expiraEm = expiraEm;
    }

    public void adicionarItem(TicketBatch lote, int quantidade) {
        validarLote(lote);
        validarQuantidade(quantidade);
        validarLoteNaoDuplicado(lote);

        itens.add(new ReservationItem(this, lote, quantidade));
    }

    private static void validarUsuario(User usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("O usuário da reserva é obrigatório");
        }
    }

    private static void validarExpiracao(OffsetDateTime expiraEm, OffsetDateTime agora) {
        if (expiraEm == null) {
            throw new IllegalArgumentException("A data de expiração da reserva é obrigatória");
        }
        if (agora == null) {
            throw new IllegalArgumentException("O momento de criação da reserva é obrigatório");
        }
        if (!expiraEm.isAfter(agora)) {
            throw new IllegalArgumentException("A data de expiração deve ser posterior à criação da reserva");
        }
    }

    private static void validarLote(TicketBatch lote) {
        if (lote == null) {
            throw new IllegalArgumentException("O lote do item é obrigatório");
        }
    }

    private static void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade do item deve ser maior que zero");
        }
    }

    private void validarLoteNaoDuplicado(TicketBatch lote) {
        boolean loteJaAdicionado = itens.stream()
                .anyMatch(item -> item.obterLote().obterId().equals(lote.obterId()));
        if (loteJaAdicionado) {
            throw new IllegalArgumentException("O lote já foi adicionado à reserva");
        }
    }

    public UUID obterId() {
        return id;
    }

    public User obterUsuario() {
        return usuario;
    }

    public ReservationStatus obterStatus() {
        return status;
    }

    public OffsetDateTime obterExpiraEm() {
        return expiraEm;
    }

    public List<ReservationItem> obterItens() {
        return List.copyOf(itens);
    }

    public OffsetDateTime obterCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime obterAtualizadoEm() {
        return atualizadoEm;
    }

    public void confirmar() {
        if (status != ReservationStatus.PENDENTE) {
            throw new IllegalStateException("Somente reservas pendentes podem ser confirmadas");
        }
        this.status = ReservationStatus.CONFIRMADA;
    }
}
