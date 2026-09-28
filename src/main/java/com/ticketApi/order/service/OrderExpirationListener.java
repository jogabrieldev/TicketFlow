package com.ticketApi.order.service;

import com.ticketApi.order.entity.OrderStatus;
import com.ticketApi.order.repository.OrderRepository;
import com.ticketApi.reservation.event.ReservationExpiredEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderExpirationListener {

    private final OrderRepository repositorioDePedidos;

    public OrderExpirationListener(OrderRepository repositorioDePedidos) {
        this.repositorioDePedidos = repositorioDePedidos;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoExpirarReserva(ReservationExpiredEvent evento) {
        repositorioDePedidos.cancelarPendenteDaReserva(
                evento.reservaId(),
                OrderStatus.PENDENTE_PAGAMENTO,
                OrderStatus.CANCELADO,
                evento.ocorridoEm()
        );
    }
}
