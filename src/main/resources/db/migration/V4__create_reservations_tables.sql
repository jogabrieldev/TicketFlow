CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservations_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_reservations_valid_status
        CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'EXPIRADA', 'CANCELADA')),
    CONSTRAINT ck_reservations_valid_expiration
        CHECK (expires_at > created_at)
);

CREATE TABLE reservation_items (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL,
    ticket_batch_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_reservation_items_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_items_ticket_batch
        FOREIGN KEY (ticket_batch_id) REFERENCES ticket_batches (id) ON DELETE RESTRICT,
    CONSTRAINT ck_reservation_items_positive_quantity
        CHECK (quantity > 0),
    CONSTRAINT ck_reservation_items_positive_unit_price
        CHECK (unit_price > 0),
    CONSTRAINT uk_reservation_items_reservation_batch
        UNIQUE (reservation_id, ticket_batch_id)
);

CREATE INDEX idx_reservations_user_id ON reservations (user_id);
CREATE INDEX idx_reservations_status_expiration ON reservations (status, expires_at);
CREATE INDEX idx_reservation_items_ticket_batch_id ON reservation_items (ticket_batch_id);
