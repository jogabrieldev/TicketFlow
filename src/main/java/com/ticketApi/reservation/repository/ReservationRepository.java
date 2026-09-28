package com.ticketApi.reservation.repository;

import com.ticketApi.reservation.entity.Reservation;
import com.ticketApi.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("SELECT reserva FROM Reservation reserva WHERE reserva.usuario.id = :usuarioId")
    Page<Reservation> buscarPorUsuarioId(@Param("usuarioId") UUID usuarioId, Pageable paginacao);

    @Query("""
            SELECT reserva.id
              FROM Reservation reserva
             WHERE reserva.status = :status
               AND reserva.expiraEm <= :agora
             ORDER BY reserva.expiraEm ASC, reserva.id ASC
            """)
    List<UUID> buscarIdsParaExpiracao(
            @Param("status") ReservationStatus status,
            @Param("agora") OffsetDateTime agora,
            Pageable limite
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Reservation reserva
               SET reserva.status = :statusExpirado,
                   reserva.atualizadoEm = :agora
             WHERE reserva.id = :reservaId
               AND reserva.status = :statusPendente
               AND reserva.expiraEm <= :agora
            """)
    int marcarComoExpiradaSePendente(
            @Param("reservaId") UUID reservaId,
            @Param("statusPendente") ReservationStatus statusPendente,
            @Param("statusExpirado") ReservationStatus statusExpirado,
            @Param("agora") OffsetDateTime agora
    );

    @Query("""
            SELECT DISTINCT reserva
              FROM Reservation reserva
              LEFT JOIN FETCH reserva.itens item
              LEFT JOIN FETCH item.lote
             WHERE reserva.id = :reservaId
            """)
    Optional<Reservation> buscarComItensPorId(@Param("reservaId") UUID reservaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT reserva FROM Reservation reserva WHERE reserva.id = :reservaId")
    Optional<Reservation> buscarPorIdParaAtualizacao(@Param("reservaId") UUID reservaId);
}
