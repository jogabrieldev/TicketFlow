package com.ticketApi.ticket.entity;

import com.ticketApi.event.entity.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_batches")
public class TicketBatch {

    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("9999999999.99");

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "O evento do lote é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private Event evento;

    @NotBlank(message = "O nome do lote é obrigatório")
    @Size(max = 255, message = "O nome do lote deve ter no máximo 255 caracteres")
    @Column(name = "name", nullable = false)
    private String nome;

    @NotNull(message = "O preço do lote é obrigatório")
    @DecimalMin(value = "0.00", inclusive = false, message = "O preço do lote deve ser maior que zero")
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Positive(message = "A quantidade total do lote deve ser maior que zero")
    @Column(name = "total_quantity", nullable = false)
    private int quantidadeTotal;

    @PositiveOrZero(message = "A quantidade disponível do lote não pode ser negativa")
    @Column(name = "available_quantity", nullable = false)
    private int quantidadeDisponivel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected TicketBatch() {
    }

    public TicketBatch(Event evento, String nome, BigDecimal preco, int quantidadeTotal) {
        validarEvento(evento);
        validarNome(nome);
        validarPreco(preco);
        validarQuantidade(quantidadeTotal);

        this.id = UUID.randomUUID();
        this.evento = evento;
        this.nome = nome.trim();
        this.preco = preco.setScale(2, RoundingMode.UNNECESSARY);
        this.quantidadeTotal = quantidadeTotal;
        this.quantidadeDisponivel = quantidadeTotal;
    }

    private static void validarEvento(Event evento) {
        if (evento == null) {
            throw new IllegalArgumentException("O evento do lote é obrigatório");
        }
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do lote é obrigatório");
        }
        if (nome.trim().length() > 255) {
            throw new IllegalArgumentException("O nome do lote deve ter no máximo 255 caracteres");
        }
    }

    private static void validarPreco(BigDecimal preco) {
        if (preco == null) {
            throw new IllegalArgumentException("O preço do lote é obrigatório");
        }
        if (preco.signum() <= 0) {
            throw new IllegalArgumentException("O preço do lote deve ser maior que zero");
        }
        if (preco.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("O preço do lote deve ter no máximo duas casas decimais");
        }
        if (preco.compareTo(PRECO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O preço do lote excede o valor máximo permitido");
        }
    }

    private static void validarQuantidade(int quantidadeTotal) {
        if (quantidadeTotal <= 0) {
            throw new IllegalArgumentException("A quantidade total do lote deve ser maior que zero");
        }
    }

    public UUID obterId() {
        return id;
    }

    public Event obterEvento() {
        return evento;
    }

    public String obterNome() {
        return nome;
    }

    public BigDecimal obterPreco() {
        return preco;
    }

    public int obterQuantidadeTotal() {
        return quantidadeTotal;
    }

    public int obterQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public OffsetDateTime obterCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime obterAtualizadoEm() {
        return atualizadoEm;
    }
}
