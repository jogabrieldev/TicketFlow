package com.ticketApi.order.entity;

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
        name = "order_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_items_order_batch",
                columnNames = {"order_id", "ticket_batch_id"}
        )
)
public class OrderItem {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "O pedido do item é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order pedido;

    @NotNull(message = "O lote do item é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_batch_id", nullable = false, updatable = false)
    private TicketBatch lote;

    @Positive(message = "A quantidade do item deve ser maior que zero")
    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantidade;

    @NotNull(message = "O preço unitário do item é obrigatório")
    @DecimalMin(value = "0.00", inclusive = false, message = "O preço unitário deve ser maior que zero")
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal precoUnitario;

    protected OrderItem() {
    }

    OrderItem(Order pedido, TicketBatch lote, int quantidade, BigDecimal precoUnitario) {
        if (pedido == null || lote == null || precoUnitario == null) {
            throw new IllegalArgumentException("Os dados do item do pedido são obrigatórios");
        }
        if (quantidade <= 0 || precoUnitario.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade e preço do item devem ser maiores que zero");
        }
        this.id = UUID.randomUUID();
        this.pedido = pedido;
        this.lote = lote;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public UUID obterId() {
        return id;
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
