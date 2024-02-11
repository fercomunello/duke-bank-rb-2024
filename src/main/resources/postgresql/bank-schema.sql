/*
##### Rinha de Backend ######
#   Revanche dos Javeiros   #
#############################
*/

DROP SCHEMA IF EXISTS api CASCADE;
CREATE SCHEMA api;

SET search_path TO api;
ALTER DATABASE rinhadb SET search_path TO api;

SET TIME ZONE 'UTC';

CREATE TYPE TXTYPE AS ENUM ('c', 'd');

CREATE TABLE bank_accounts (
    id                BIGSERIAL,
    credit_limit      INT NOT NULL DEFAULT 0 CHECK ( credit_limit >= 0 ),
    balance           INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE bank_transactions (
    id              BIGSERIAL,
    account_id      BIGINT NOT NULL,
    type            TXTYPE NOT NULL,
    amount          INT NOT NULL CHECK ( amount > 0 ),
    description     VARCHAR(10),
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id),
    FOREIGN KEY (account_id) REFERENCES bank_accounts (id)
);

CREATE INDEX idx_tx_account_id ON bank_transactions (account_id);
CREATE INDEX idx_tx_issued_filter ON bank_transactions (issued_at DESC);

CREATE OR REPLACE FUNCTION process_bank_transaction(
    p_account_id BIGINT,
    p_type TXTYPE,
    p_amount INT DEFAULT 0,
    p_description VARCHAR(10) DEFAULT NULL
)
RETURNS TABLE (o_credit_limit INT, o_balance INT) AS $$
DECLARE
    v_credit_limit INT NOT NULL DEFAULT 0;
    v_balance      INT NOT NULL DEFAULT 0;
    v_lock_account BOOLEAN DEFAULT FALSE;
BEGIN
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

    IF (p_account_id IS NULL OR p_type IS NULL OR p_amount <= 0) THEN
        RAISE NOTICE 'TX must be associated with an account,
         it must has a type (C-Credit | D-Debit) and amount.';
        ROLLBACK;
    END IF;

    IF (p_type = 'c'::TXTYPE) THEN
        UPDATE bank_accounts SET balance = (balance + p_amount)
        WHERE (id = p_account_id)
        RETURNING credit_limit, balance INTO v_credit_limit, v_balance;
    ELSE
        v_lock_account := TRUE;
    END IF;

    IF (v_lock_account) THEN
        SELECT c.credit_limit,
               c.balance
        INTO v_credit_limit,
             v_balance
        FROM bank_accounts c
        WHERE (c.id = p_account_id)
        FOR UPDATE;

        IF (FOUND AND p_type = 'd'::TXTYPE) THEN
            IF ((v_balance - p_amount) < -v_credit_limit) THEN
                RAISE NOTICE 'Debit transaction rejected,
                 insufficient credit limit for this account.';
                ROLLBACK;
            END IF;

            v_balance := (v_balance - p_amount);

            UPDATE bank_accounts c SET balance = v_balance WHERE (c.id = p_account_id);
        END IF;
    END IF;

    INSERT INTO bank_transactions (account_id, type, amount, description)
        VALUES (p_account_id, p_type, p_amount, p_description);

    o_credit_limit := v_credit_limit;
    o_balance := v_balance;

    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

GRANT INSERT ON bank_accounts TO duke;
GRANT UPDATE ON bank_accounts TO duke;
REVOKE DELETE ON bank_accounts FROM duke;

GRANT INSERT ON bank_transactions TO duke;
REVOKE UPDATE ON bank_transactions FROM duke;
REVOKE DELETE ON bank_transactions FROM duke;

INSERT INTO bank_accounts
       (id, credit_limit, balance)
VALUES (1, 1000 * 100, 0), -- R$1,000.00
       (2, 800 * 100, 0), -- R$8,000.00
       (3, 10000 * 100, 0), -- R$10,000.00
       (4, 100000 * 100, 0), -- R$100,000.00
       (5, 5000 * 100, 0); -- R$5,000.00