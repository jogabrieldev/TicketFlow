package com.ticketApi.payment.repository;

import com.ticketApi.payment.entity.Payment;
import com.ticketApi.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByPedidoIdAndStatusIn(UUID pedidoId, Collection<PaymentStatus> statuses);

    @Query("""
        SELECT pagamento
          FROM Payment pagamento
        JOIN FETCH pagamento.pedido pedido
          WHERE pagamento.id = :pagamentoId
          AND pedido.usuario.id = :usuarioId
        """)
    Optional<Payment> buscarDoUsuario(@Param("pagamentoId") UUID pagamentoId, @Param("usuarioId") UUID usuarioId);
}
