package com.ticketApi.order.service;

import com.ticketApi.order.entity.OrderStatus;
import com.ticketApi.order.repository.OrderRepository;
import com.ticketApi.reservation.event.ReservationExpiredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderExpirationListenerTest {

    @Mock
    private OrderRepository repositorioDePedidos;

    @Test
    void deveCancelarPedidoPendenteDaReservaExpirada() {
        UUID reservaId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2030-01-01T12:00:00Z");
        OrderExpirationListener listener = new OrderExpirationListener(repositorioDePedidos);

        listener.aoExpirarReserva(new ReservationExpiredEvent(reservaId, agora));

        verify(repositorioDePedidos).cancelarPendenteDaReserva(
                reservaId,
                OrderStatus.PENDENTE_PAGAMENTO,
                OrderStatus.CANCELADO,
                agora
        );
    }
}
