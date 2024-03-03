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
    credit_limit      BIGINT NOT NULL DEFAULT 0 CHECK ( credit_limit >= 0 ),
    balance           BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE bank_transactions (
    id              BIGSERIAL,
    account_id      BIGINT NOT NULL,
    type            TXTYPE NOT NULL,
    amount          BIGINT NOT NULL CHECK ( amount > 0 ),
    description     VARCHAR(10) CHECK ( description IS NULL OR length(description) <= 10 ),
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id),
    FOREIGN KEY (account_id) REFERENCES bank_accounts (id)
);

CREATE INDEX idx_tx_account_id ON bank_transactions (account_id);
CREATE INDEX idx_tx_issued_filter ON bank_transactions (issued_at DESC);

CREATE OR REPLACE FUNCTION process_bank_transaction(
    p_account_id BIGINT,
    p_type TXTYPE,
    p_amount BIGINT,
    p_description VARCHAR(10)
)
RETURNS TABLE (o_tx_performed BOOL,
               o_credit_limit INT,
               o_balance BIGINT) AS $$
DECLARE
    v_credit_limit BIGINT NOT NULL DEFAULT 0;
    v_balance      BIGINT NOT NULL DEFAULT 0;
    v_lock_account BOOLEAN DEFAULT FALSE;
BEGIN
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

    IF (p_account_id IS NULL OR p_type IS NULL OR p_amount <= 0) THEN
        RAISE check_violation USING MESSAGE =
          'TX must be associated with an account, it must has a type (C-Credit | D-Debit) and an amount.',
          HINT = 'Check the validation steps on the application side';
    END IF;

    IF (p_type = 'c'::TXTYPE) THEN
        UPDATE bank_accounts SET balance = (balance + p_amount)
        WHERE (id = p_account_id)
        RETURNING credit_limit, balance INTO v_credit_limit, v_balance;

        o_tx_performed := TRUE;
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
            v_balance := (v_balance - p_amount);
            IF (v_balance < -v_credit_limit) THEN
                /* Debit transaction rejected, insufficient
                   credit creditLimit for this account. */
                o_tx_performed := FALSE;
            ELSE
                UPDATE bank_accounts account SET balance = v_balance
                WHERE (account.id = p_account_id);

                o_tx_performed := TRUE;
            END IF;
        END IF;
    END IF;

    IF (o_tx_performed) THEN
        INSERT INTO bank_transactions (account_id, type, amount, description)
            VALUES (p_account_id, p_type, p_amount, p_description);
    END IF;

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
       (credit_limit, balance)
VALUES (1000 * 100, 0),   -- 1 | $1,000.00 == 100000
       (80000 * 100, 0),  -- 2 | $80,000.00 == 80000
       (10000 * 100, 0),  -- 3 | $10,000.00 == 1000000
       (100000 * 100, 0), -- 4 | $100,000.00 == 10000000
       (5000 * 100, 0);   -- 5 | $5,000.00 == 500000