package com.ticketApi.payment.entity;

import com.ticketApi.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order pedido;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(name = "amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal valor;

    @NotBlank
    @Column(name = "gateway_reference", nullable = false, length = 100, updatable = false, unique = true)
    private String referenciaGateway;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Payment() {
    }

    public Payment(Order pedido, PaymentStatus status, String referenciaGateway) {
        if (pedido == null) {
            throw new IllegalArgumentException("O pedido do pagamento é obrigatório");
        }
        if (status == null) {
            throw new IllegalArgumentException("O status do pagamento é obrigatório");
        }
        if (referenciaGateway == null || referenciaGateway.isBlank()) {
            throw new IllegalArgumentException("A referência do gateway é obrigatória");
        }
        this.id = UUID.randomUUID();
        this.pedido = pedido;
        this.status = status;
        this.valor = pedido.obterValorTotal();
        this.referenciaGateway = referenciaGateway;
    }

    public UUID obterId() {
        return id;
    }

    public Order obterPedido() {
        return pedido;
    }

    public PaymentStatus obterStatus() {
        return status;
    }

    public BigDecimal obterValor() {
        return valor;
    }

    public String obterReferenciaGateway() {
        return referenciaGateway;
    }

    public OffsetDateTime obterCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime obterAtualizadoEm() {
        return atualizadoEm;
    }
}
