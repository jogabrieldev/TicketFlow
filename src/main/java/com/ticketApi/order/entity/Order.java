package com.ticketApi.order.entity;

import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.entity.ReservationItem;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_orders_reservation", columnNames = "reservation_id")
)
public class Order {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999999.99");

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "O usuário do pedido é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User usuario;

    @NotNull(message = "A reserva do pedido é obrigatória")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, updatable = false, unique = true)
    private Reservation reserva;

    @NotNull(message = "O status do pedido é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @NotNull(message = "O valor total do pedido é obrigatório")
    @DecimalMin(value = "0.00", inclusive = false, message = "O valor total deve ser maior que zero")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal valorTotal;

    @NotNull(message = "O limite de pagamento é obrigatório")
    @Column(name = "payment_deadline", nullable = false, updatable = false)
    private OffsetDateTime limitePagamento;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> itens = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Order() {
    }

    public Order(User usuario, Reservation reserva) {
        if (usuario == null) {
            throw new IllegalArgumentException("O usuário do pedido é obrigatório");
        }
        if (reserva == null) {
            throw new IllegalArgumentException("A reserva do pedido é obrigatória");
        }
        if (reserva.obterItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido deve possuir pelo menos um item");
        }

        this.id = UUID.randomUUID();
        this.usuario = usuario;
        this.reserva = reserva;
        this.status = OrderStatus.PENDENTE_PAGAMENTO;
        this.limitePagamento = reserva.obterExpiraEm();
        reserva.obterItens().forEach(this::adicionarItem);
        this.valorTotal = calcularValorTotal();
        if (valorTotal.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O valor total do pedido excede o valor máximo permitido");
        }
    }

    private void adicionarItem(ReservationItem itemDaReserva) {
        itens.add(new OrderItem(
                this,
                itemDaReserva.obterLote(),
                itemDaReserva.obterQuantidade(),
                itemDaReserva.obterPrecoUnitario()
        ));
    }

    private BigDecimal calcularValorTotal() {
        return itens.stream()
                .map(OrderItem::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID obterId() {
        return id;
    }

    public User obterUsuario() {
        return usuario;
    }

    public Reservation obterReserva() {
        return reserva;
    }

    public OrderStatus obterStatus() {
        return status;
    }

    public BigDecimal obterValorTotal() {
        return valorTotal;
    }

    public OffsetDateTime obterLimitePagamento() {
        return limitePagamento;
    }

    public List<OrderItem> obterItens() {
        return List.copyOf(itens);
    }

    public OffsetDateTime obterCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime obterAtualizadoEm() {
        return atualizadoEm;
    }

    public void marcarComoPago() {
        if (status != OrderStatus.PENDENTE_PAGAMENTO) {
            throw new IllegalStateException("Somente pedidos pendentes podem ser pagos");
        }
        this.status = OrderStatus.PAGO;
    }
}
