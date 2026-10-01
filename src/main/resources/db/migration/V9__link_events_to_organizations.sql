DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM events) THEN
        RAISE EXCEPTION
            'A migration V9 exige que a tabela events esteja vazia para vincular organizações obrigatórias';
    END IF;
END $$;

ALTER TABLE events
    ADD COLUMN organization_id UUID NOT NULL;

ALTER TABLE events
    ADD CONSTRAINT fk_events_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT;

CREATE INDEX idx_events_organization_id ON events (organization_id);
