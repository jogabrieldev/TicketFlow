CREATE TABLE events (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_events_name_not_blank CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_events_location_not_blank CHECK (LENGTH(TRIM(location)) > 0),
    CONSTRAINT ck_events_valid_period CHECK (ends_at > starts_at)
);
