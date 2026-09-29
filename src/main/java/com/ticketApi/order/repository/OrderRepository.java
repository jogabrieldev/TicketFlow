package com.ticketApi.order.repository;

import com.ticketApi.order.entity.Order;
import com.ticketApi.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByReservaId(UUID reservaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pedido FROM Order pedido WHERE pedido.id = :pedidoId")
    java.util.Optional<Order> buscarPorIdParaAtualizacao(@Param("pedidoId") UUID pedidoId);

    @Query("SELECT pedido FROM Order pedido WHERE pedido.usuario.id = :usuarioId")
    Page<Order> buscarPorUsuarioId(@Param("usuarioId") UUID usuarioId, Pageable paginacao);

    @Query("""
            SELECT DISTINCT pedido
              FROM Order pedido
              LEFT JOIN FETCH pedido.itens item
              LEFT JOIN FETCH item.lote
             WHERE pedido.id IN :identificadores
            """)
    List<Order> buscarComItensPorIds(@Param("identificadores") Collection<UUID> identificadores);

    @Query("""
            SELECT DISTINCT pedido
              FROM Order pedido
              LEFT JOIN FETCH pedido.itens item
              LEFT JOIN FETCH item.lote
             WHERE pedido.id = :pedidoId
            """)
    java.util.Optional<Order> buscarComItensPorId(@Param("pedidoId") UUID pedidoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Order pedido
               SET pedido.status = :statusCancelado,
                   pedido.atualizadoEm = :agora
             WHERE pedido.reserva.id = :reservaId
               AND pedido.status = :statusPendente
            """)
    int cancelarPendenteDaReserva(
            @Param("reservaId") UUID reservaId,
            @Param("statusPendente") OrderStatus statusPendente,
            @Param("statusCancelado") OrderStatus statusCancelado,
            @Param("agora") OffsetDateTime agora
    );
}
