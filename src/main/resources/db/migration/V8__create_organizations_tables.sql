CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    trade_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(11) NOT NULL,
    street VARCHAR(255) NOT NULL,
    address_number VARCHAR(20) NOT NULL,
    address_complement VARCHAR(255),
    neighborhood VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state CHAR(2) NOT NULL,
    postal_code VARCHAR(8) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_organizations_trade_name_not_blank
        CHECK (LENGTH(TRIM(trade_name)) > 0),
    CONSTRAINT ck_organizations_legal_name_not_blank
        CHECK (LENGTH(TRIM(legal_name)) > 0),
    CONSTRAINT ck_organizations_cnpj_format
        CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_organizations_email_not_blank
        CHECK (LENGTH(TRIM(email)) > 0),
    CONSTRAINT ck_organizations_phone_format
        CHECK (phone ~ '^[0-9]{10,11}$'),
    CONSTRAINT ck_organizations_street_not_blank
        CHECK (LENGTH(TRIM(street)) > 0),
    CONSTRAINT ck_organizations_address_number_not_blank
        CHECK (LENGTH(TRIM(address_number)) > 0),
    CONSTRAINT ck_organizations_neighborhood_not_blank
        CHECK (LENGTH(TRIM(neighborhood)) > 0),
    CONSTRAINT ck_organizations_city_not_blank
        CHECK (LENGTH(TRIM(city)) > 0),
    CONSTRAINT ck_organizations_state_format
        CHECK (state ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_organizations_postal_code_format
        CHECK (postal_code ~ '^[0-9]{8}$')
);

CREATE UNIQUE INDEX uk_organizations_cnpj ON organizations (cnpj);

CREATE TABLE organization_members (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_organization_members_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_organization_members_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_organization_members_organization_user
        UNIQUE (organization_id, user_id),
    CONSTRAINT ck_organization_members_valid_role
        CHECK (role IN ('PROPRIETARIO'))
);

CREATE UNIQUE INDEX uk_organization_members_owner_organization
    ON organization_members (organization_id)
    WHERE role = 'PROPRIETARIO';

CREATE UNIQUE INDEX uk_organization_members_owner_user
    ON organization_members (user_id)
    WHERE role = 'PROPRIETARIO';

CREATE INDEX idx_organization_members_organization_id
    ON organization_members (organization_id);

CREATE INDEX idx_organization_members_user_id
    ON organization_members (user_id);
