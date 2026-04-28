
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE access_level_enum AS ENUM ('USER', 'ADMIN');

CREATE TYPE standing_enum AS ENUM ('GOOD', 'FAIR', 'POOR', 'SUSPENDED');

CREATE TYPE admin_role_enum AS ENUM ('SUPER_ADMIN', 'BRANCH_MANAGER', 'SUPPORT');

CREATE TYPE account_type_enum AS ENUM ('CHECKING', 'SAVINGS');

CREATE TYPE account_status_enum AS ENUM ('ACTIVE', 'CLOSED', 'FROZEN');



CREATE TABLE users (

    user_id         SERIAL          PRIMARY KEY,

    full_name       VARCHAR(120)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    standing        standing_enum   NOT NULL DEFAULT 'GOOD',
    access_level    access_level_enum NOT NULL DEFAULT 'USER',

    admin_role      admin_role_enum,

    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email       UNIQUE (email),

    CONSTRAINT chk_admin_role_consistency CHECK (
        (access_level = 'ADMIN' AND admin_role IS NOT NULL) OR
        (access_level = 'USER'  AND admin_role IS NULL)
    )
);

CREATE INDEX idx_users_full_name   ON users (full_name);
CREATE INDEX idx_users_access_level ON users (access_level);
CREATE INDEX idx_users_standing    ON users (standing);


CREATE TABLE accounts (
    account_id      SERIAL              PRIMARY KEY,

    owner_id        INTEGER             NOT NULL,

    balance         NUMERIC(15, 2)      NOT NULL DEFAULT 0.00,
    account_type    account_type_enum   NOT NULL,
    status          account_status_enum NOT NULL DEFAULT 'ACTIVE',

    overdraft_limit NUMERIC(15, 2),

    interest_rate   NUMERIC(5, 4),      

    created_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),

    CONSTRAINT fk_accounts_owner
        FOREIGN KEY (owner_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_savings_no_negative CHECK (
        account_type != 'SAVINGS' OR balance >= 0
    ),

    CONSTRAINT chk_overdraft_consistency CHECK (
        (account_type = 'CHECKING' AND overdraft_limit IS NOT NULL) OR
        (account_type = 'SAVINGS'  AND overdraft_limit IS NULL)
    ),

    CONSTRAINT chk_interest_consistency CHECK (
        (account_type = 'SAVINGS'  AND interest_rate IS NOT NULL) OR
        (account_type = 'CHECKING' AND interest_rate IS NULL)
    ),

    CONSTRAINT chk_overdraft_positive CHECK (
        overdraft_limit IS NULL OR overdraft_limit >= 0
    ),

    CONSTRAINT chk_interest_positive CHECK (
        interest_rate IS NULL OR interest_rate >= 0
    ),

    CONSTRAINT chk_checking_overdraft_balance CHECK (
        account_type != 'CHECKING' OR balance >= -overdraft_limit
    )
);

CREATE INDEX idx_accounts_owner_id     ON accounts (owner_id);
CREATE INDEX idx_accounts_type         ON accounts (account_type);
CREATE INDEX idx_accounts_status       ON accounts (status);



CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION fn_set_updated_at();


INSERT INTO users (full_name, email, standing, access_level, admin_role)
VALUES ('System Administrator', 'admin@bankpanel.com', 'GOOD', 'ADMIN', 'SUPER_ADMIN');

INSERT INTO users (full_name, email, standing, access_level)
VALUES
    ('Alice Johnson', 'alice@example.com', 'GOOD', 'USER'),
    ('Bob Williams',  'bob@example.com',   'FAIR', 'USER');

INSERT INTO accounts (owner_id, balance, account_type, status, overdraft_limit)
VALUES (2, 2500.00, 'CHECKING', 'ACTIVE', 500.00);

INSERT INTO accounts (owner_id, balance, account_type, status, interest_rate)
VALUES (2, 10000.00, 'SAVINGS', 'ACTIVE', 0.0300);

INSERT INTO accounts (owner_id, balance, account_type, status, overdraft_limit)
VALUES (3, 750.50, 'CHECKING', 'ACTIVE', 500.00);
