CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    operation VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    resource_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_idempotency_records_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_idempotency_records_key_not_blank
        CHECK (LENGTH(TRIM(idempotency_key)) > 0),
    CONSTRAINT ck_idempotency_records_valid_operation
        CHECK (operation IN ('CRIAR_RESERVA', 'CRIAR_PEDIDO', 'CRIAR_PAGAMENTO')),
    CONSTRAINT ck_idempotency_records_hash_format
        CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT uk_idempotency_records_scope
        UNIQUE (user_id, operation, idempotency_key)
);

CREATE INDEX idx_idempotency_records_created_at ON idempotency_records (created_at);
