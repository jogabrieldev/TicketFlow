CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_users_name_not_blank
        CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_users_email_not_blank
        CHECK (LENGTH(TRIM(email)) > 0),
    CONSTRAINT ck_users_password_hash_not_blank
        CHECK (LENGTH(TRIM(password_hash)) > 0),
    CONSTRAINT ck_users_valid_role
        CHECK (role IN ('CLIENTE', 'ADMINISTRADOR'))
);

CREATE UNIQUE INDEX uk_users_email_normalized ON users (LOWER(email));
