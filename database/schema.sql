-- Bank Administration Panel — PostgreSQL Schema (Supabase-ready)
-- ============================================================================
-- Inheritance strategy: Single Table Inheritance (STI) for both hierarchies.
--   • User/Admin   → single "users"    table   (discriminator: access_level)
--   • Account types → single "accounts" table  (discriminator: account_type)
--
-- Run this entire script in the Supabase SQL Editor in one go.

-- 0. Enable pgcrypto for gen_random_uuid() if not already enabled
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. ENUM TYPES

-- User access levels — acts as the STI discriminator for User vs Admin
CREATE TYPE access_level_enum AS ENUM ('USER', 'ADMIN');

-- User standing — enforced domain values
CREATE TYPE standing_enum AS ENUM ('GOOD', 'FAIR', 'POOR', 'SUSPENDED');

-- Admin role specialisations
CREATE TYPE admin_role_enum AS ENUM ('SUPER_ADMIN', 'BRANCH_MANAGER', 'SUPPORT');

-- Account type — acts as the STI discriminator for account subclasses
CREATE TYPE account_type_enum AS ENUM ('CHECKING', 'SAVINGS');

-- Account status
CREATE TYPE account_status_enum AS ENUM ('ACTIVE', 'CLOSED', 'FROZEN');


-- 2. USERS TABLE  (covers both User and Admin via STI)
-- When access_level = 'USER'  → admin_role is NULL
-- When access_level = 'ADMIN' → admin_role is required

CREATE TABLE users (
    -- Primary key: auto-incrementing integer matching the UML "int userId"
    user_id         SERIAL          PRIMARY KEY,

    -- Shared User fields
    full_name       VARCHAR(120)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    standing        standing_enum   NOT NULL DEFAULT 'GOOD',
    access_level    access_level_enum NOT NULL DEFAULT 'USER',

    -- Admin-only field (NULL for regular users)
    admin_role      admin_role_enum,

    -- Audit timestamps
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    -- Constraints
    CONSTRAINT uq_users_email       UNIQUE (email),

    -- Ensure admin_role is set when and only when the user is an ADMIN
    CONSTRAINT chk_admin_role_consistency CHECK (
        (access_level = 'ADMIN' AND admin_role IS NOT NULL) OR
        (access_level = 'USER'  AND admin_role IS NULL)
    )
);

-- Index for fast lookups
CREATE INDEX idx_users_full_name   ON users (full_name);
CREATE INDEX idx_users_access_level ON users (access_level);
CREATE INDEX idx_users_standing    ON users (standing);


-- 3. ACCOUNTS TABLE  (covers Account, CheckingAccount, SavingsAccount via STI)
-- When account_type = 'CHECKING' → overdraft_limit is required, interest_rate is NULL
-- When account_type = 'SAVINGS'  → interest_rate is required, overdraft_limit is NULL

CREATE TABLE accounts (
    -- Primary key: auto-incrementing integer matching the UML "int accountId"
    account_id      SERIAL              PRIMARY KEY,

    -- Foreign key — enforces the 1-to-many User → Accounts relationship
    owner_id        INTEGER             NOT NULL,

    -- Shared Account fields
    balance         NUMERIC(15, 2)      NOT NULL DEFAULT 0.00,
    account_type    account_type_enum   NOT NULL,
    status          account_status_enum NOT NULL DEFAULT 'ACTIVE',

    -- CheckingAccount-only field
    overdraft_limit NUMERIC(15, 2),

    -- SavingsAccount-only field
    interest_rate   NUMERIC(5, 4),      -- e.g. 0.0300 = 3%

    -- Audit timestamps
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),

    -- Foreign key constraint
    CONSTRAINT fk_accounts_owner
        FOREIGN KEY (owner_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE,

    -- Balance can go negative for checking (up to overdraft), but never for savings
    CONSTRAINT chk_savings_no_negative CHECK (
        account_type != 'SAVINGS' OR balance >= 0
    ),

    -- Ensure overdraft_limit is set when and only when the account is CHECKING
    CONSTRAINT chk_overdraft_consistency CHECK (
        (account_type = 'CHECKING' AND overdraft_limit IS NOT NULL) OR
        (account_type = 'SAVINGS'  AND overdraft_limit IS NULL)
    ),

    -- Ensure interest_rate is set when and only when the account is SAVINGS
    CONSTRAINT chk_interest_consistency CHECK (
        (account_type = 'SAVINGS'  AND interest_rate IS NOT NULL) OR
        (account_type = 'CHECKING' AND interest_rate IS NULL)
    ),

    -- Overdraft limit must be non-negative
    CONSTRAINT chk_overdraft_positive CHECK (
        overdraft_limit IS NULL OR overdraft_limit >= 0
    ),

    -- Interest rate must be non-negative
    CONSTRAINT chk_interest_positive CHECK (
        interest_rate IS NULL OR interest_rate >= 0
    ),

    -- Checking balance cannot exceed overdraft limit below zero
    CONSTRAINT chk_checking_overdraft_balance CHECK (
        account_type != 'CHECKING' OR balance >= -overdraft_limit
    )
);

-- Index for the FK — speeds up joins and lookups by owner
CREATE INDEX idx_accounts_owner_id     ON accounts (owner_id);
CREATE INDEX idx_accounts_type         ON accounts (account_type);
CREATE INDEX idx_accounts_status       ON accounts (status);


-- 4. AUTO-UPDATE updated_at TRIGGER

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


-- IMPORTANT!! 5. SEED DATA (optional — remove before production)

-- Insert a super admin
INSERT INTO users (full_name, email, standing, access_level, admin_role)
VALUES ('System Administrator', 'admin@bankpanel.com', 'GOOD', 'ADMIN', 'SUPER_ADMIN');

-- Insert two regular users
INSERT INTO users (full_name, email, standing, access_level)
VALUES
    ('Alice Johnson', 'alice@example.com', 'GOOD', 'USER'),
    ('Bob Williams',  'bob@example.com',   'FAIR', 'USER');

-- Create accounts for Alice (user_id = 2)
INSERT INTO accounts (owner_id, balance, account_type, status, overdraft_limit)
VALUES (2, 2500.00, 'CHECKING', 'ACTIVE', 500.00);

INSERT INTO accounts (owner_id, balance, account_type, status, interest_rate)
VALUES (2, 10000.00, 'SAVINGS', 'ACTIVE', 0.0300);

-- Create a checking account for Bob (user_id = 3)
INSERT INTO accounts (owner_id, balance, account_type, status, overdraft_limit)
VALUES (3, 750.50, 'CHECKING', 'ACTIVE', 500.00);
