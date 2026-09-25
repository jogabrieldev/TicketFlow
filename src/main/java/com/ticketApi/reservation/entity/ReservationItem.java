package com.ticketApi.reservation.entity;

import com.ticketApi.ticket.entity.TicketBatch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "reservation_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_items_reservation_batch",
                columnNames = {"reservation_id", "ticket_batch_id"}
        )
)
public class ReservationItem {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "A reserva do item é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, updatable = false)
    private Reservation reserva;

    @NotNull(message = "O lote do item é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_batch_id", nullable = false, updatable = false)
    private TicketBatch lote;

    @Positive(message = "A quantidade do item deve ser maior que zero")
    @Column(name = "quantity", nullable = false)
    private int quantidade;

    @NotNull(message = "O preço unitário do item é obrigatório")
    @DecimalMin(value = "0.00", inclusive = false, message = "O preço unitário deve ser maior que zero")
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal precoUnitario;

    protected ReservationItem() {
    }

    ReservationItem(Reservation reserva, TicketBatch lote, int quantidade) {
        this.id = UUID.randomUUID();
        this.reserva = reserva;
        this.lote = lote;
        this.quantidade = quantidade;
        this.precoUnitario = lote.obterPreco();
    }

    public UUID obterId() {
        return id;
    }

    public Reservation obterReserva() {
        return reserva;
    }

    public TicketBatch obterLote() {
        return lote;
    }

    public int obterQuantidade() {
        return quantidade;
    }

    public BigDecimal obterPrecoUnitario() {
        return precoUnitario;
    }
}
