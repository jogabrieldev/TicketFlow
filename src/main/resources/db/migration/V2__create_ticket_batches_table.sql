CREATE TABLE ticket_batches (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    total_quantity INTEGER NOT NULL,
    available_quantity INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_batches_event
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ticket_batches_name_not_blank
        CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_ticket_batches_positive_price
        CHECK (price > 0),
    CONSTRAINT ck_ticket_batches_positive_total_quantity
        CHECK (total_quantity > 0),
    CONSTRAINT ck_ticket_batches_valid_available_quantity
        CHECK (available_quantity >= 0 AND available_quantity <= total_quantity)
);

CREATE INDEX idx_ticket_batches_event_id ON ticket_batches (event_id);
