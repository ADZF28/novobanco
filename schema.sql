-- =============================================================================
-- NovoBanco - Schema DDL
-- PostgreSQL 16
-- =============================================================================

-- Clientes
-- id: BIGSERIAL — PK numérico autoincremental, eficiente en FK e índices.
-- uuid: identificador público único, expuesto en la API, generado en aplicación.
-- identificación (cédula, RUC, pasaporte) es el campo primario de unicidad de negocio.
-- El email es también único por requisito de contacto.
CREATE TABLE IF NOT EXISTS clients (
    id                  BIGSERIAL    PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid(),
    full_name           VARCHAR(200) NOT NULL,
    email               VARCHAR(200) NOT NULL,
    identification      VARCHAR(50)  NOT NULL,
    type_identification VARCHAR(20)  NOT NULL,
    phone               VARCHAR(20),
    address             VARCHAR(500),
    gender              VARCHAR(30),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_clients_uuid           UNIQUE (uuid),
    CONSTRAINT uq_clients_email          UNIQUE (email),
    CONSTRAINT uq_clients_identification UNIQUE (identification),
    CONSTRAINT chk_clients_type_id       CHECK (type_identification IN ('NATIONAL_ID', 'RUC', 'PASSPORT'))
);

-- Cuentas
-- id: BIGSERIAL — PK numérico autoincremental.
-- uuid: identificador público único expuesto en la API.
-- balance NUMERIC(19,4): precisión financiera sin errores de punto flotante.
-- CHECK balance >= 0: segunda línea de defensa (la primera es la capa de dominio).
CREATE TABLE IF NOT EXISTS accounts (
    id             BIGSERIAL     PRIMARY KEY,
    uuid           UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_number VARCHAR(20)   NOT NULL,
    client_id      BIGINT        NOT NULL REFERENCES clients(id),
    type           VARCHAR(20)   NOT NULL,
    currency       VARCHAR(3)    NOT NULL DEFAULT 'USD',
    balance        NUMERIC(19,4) NOT NULL DEFAULT 0,
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_accounts_uuid           UNIQUE (uuid),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number),
    CONSTRAINT chk_accounts_balance       CHECK (balance >= 0),
    CONSTRAINT chk_accounts_type          CHECK (type IN ('SAVINGS', 'CHECKING')),
    CONSTRAINT chk_accounts_status        CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT chk_accounts_currency      CHECK (currency IN ('USD'))
);

-- Transacciones
-- id: BIGSERIAL — PK numérico autoincremental.
-- uuid: identificador público único expuesto en la API.
-- reference: campo de idempotencia (UNIQUE). Para transferencias, debit y credit
-- comparten transfer_reference, lo que permite reconstruir el par sin FK circular.
CREATE TABLE IF NOT EXISTS transactions (
    id                 BIGSERIAL     PRIMARY KEY,
    uuid               UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_id         BIGINT        NOT NULL REFERENCES accounts(id),
    type               VARCHAR(30)   NOT NULL,
    amount             NUMERIC(19,4) NOT NULL,
    reference          UUID          NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    description        VARCHAR(500),
    transfer_reference UUID,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_transactions_uuid       UNIQUE (uuid),
    CONSTRAINT uq_transactions_reference  UNIQUE (reference),
    CONSTRAINT chk_transactions_type      CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_DEBIT', 'TRANSFER_CREDIT')),
    CONSTRAINT chk_transactions_status    CHECK (status IN ('SUCCESS', 'FAILED', 'REVERSED')),
    CONSTRAINT chk_transactions_amount    CHECK (amount > 0)
);

-- =============================================================================
-- Índices
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_accounts_client_id
    ON accounts(client_id);

CREATE INDEX IF NOT EXISTS idx_accounts_uuid
    ON accounts(uuid);

CREATE INDEX IF NOT EXISTS idx_clients_uuid
    ON clients(uuid);

CREATE INDEX IF NOT EXISTS idx_transactions_account_id_created_at
    ON transactions(account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_reference
    ON transactions(reference);

CREATE INDEX IF NOT EXISTS idx_transactions_uuid
    ON transactions(uuid);

CREATE INDEX IF NOT EXISTS idx_transactions_transfer_debit_created_at
    ON transactions(type, created_at)
    WHERE type = 'TRANSFER_DEBIT';

CREATE INDEX IF NOT EXISTS idx_transactions_transfer_reference
    ON transactions(transfer_reference)
    WHERE transfer_reference IS NOT NULL;
