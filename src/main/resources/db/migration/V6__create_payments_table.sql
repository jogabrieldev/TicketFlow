CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    gateway_reference VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT uk_payments_gateway_reference UNIQUE (gateway_reference),
    CONSTRAINT ck_payments_valid_status
        CHECK (status IN ('PROCESSANDO', 'APROVADO', 'RECUSADO')),
    CONSTRAINT ck_payments_positive_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_order_created_at ON payments (order_id, created_at DESC);
CREATE UNIQUE INDEX uk_payments_active_order
    ON payments (order_id)
    WHERE status IN ('PROCESSANDO', 'APROVADO');
