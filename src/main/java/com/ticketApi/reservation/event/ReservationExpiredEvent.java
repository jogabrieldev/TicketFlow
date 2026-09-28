package com.ticketApi.reservation.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationExpiredEvent(UUID reservaId, OffsetDateTime ocorridoEm) {
}
