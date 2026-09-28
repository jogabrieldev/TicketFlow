CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    reservation_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    payment_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE RESTRICT,
    CONSTRAINT uk_orders_reservation UNIQUE (reservation_id),
    CONSTRAINT ck_orders_valid_status
        CHECK (status IN ('PENDENTE_PAGAMENTO', 'PAGO', 'CANCELADO', 'REEMBOLSADO')),
    CONSTRAINT ck_orders_positive_total CHECK (total_amount > 0)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    ticket_batch_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_ticket_batch
        FOREIGN KEY (ticket_batch_id) REFERENCES ticket_batches (id) ON DELETE RESTRICT,
    CONSTRAINT ck_order_items_positive_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_positive_unit_price CHECK (unit_price > 0),
    CONSTRAINT uk_order_items_order_batch UNIQUE (order_id, ticket_batch_id)
);

CREATE INDEX idx_orders_user_created_at ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status_payment_deadline ON orders (status, payment_deadline);
CREATE INDEX idx_order_items_ticket_batch_id ON order_items (ticket_batch_id);
