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

CREATE TABLE customer_accounts (
    id                BIGSERIAL,
    credit_limit      INT NOT NULL DEFAULT 0 CHECK ( credit_limit >= 0 ),
    balance           INT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE customer_transactions (
    id              BIGSERIAL,
    account_id      BIGINT NOT NULL,
    type            TXTYPE NOT NULL,
    amount          INT NOT NULL CHECK ( amount > 0 ),
    description     VARCHAR(10),
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id),
    FOREIGN KEY (account_id) REFERENCES customer_accounts (id)
);

CREATE INDEX idx_ctx_account_id ON customer_transactions (account_id);
CREATE INDEX idx_customers_tx_filter ON customer_transactions (issued_at DESC);

CREATE OR REPLACE FUNCTION process_customer_tx(
    p_account_id BIGINT,
    p_tx_type TXTYPE,
    p_tx_amount INT DEFAULT 0,
    p_tx_description VARCHAR(10) DEFAULT NULL
)
RETURNS TABLE (o_credit_limit INT, o_balance INT) AS $$
DECLARE
    v_credit_limit INT NOT NULL DEFAULT 0;
    v_balance      INT NOT NULL DEFAULT 0;
    v_lock_account BOOLEAN DEFAULT FALSE;
BEGIN
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

    IF (p_account_id IS NULL OR p_tx_type IS NULL OR p_tx_amount <= 0) THEN
        RAISE NOTICE 'TX must be associated with an account,
         it must has a type (C-Credit | D-Debit) and amount.';
        ROLLBACK;
    END IF;

    IF (p_tx_type = 'c'::TXTYPE) THEN
        UPDATE customer_accounts
            SET balance = (balance + p_tx_amount)
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
        FROM customer_accounts c
        WHERE (c.id = p_account_id)
        FOR UPDATE;

        IF (FOUND AND p_tx_type = 'd'::TXTYPE) THEN
            IF ((v_balance - p_tx_amount) < -v_credit_limit) THEN
                RAISE NOTICE 'Debit transaction rejected,
                 insufficient credit limit for this account.';
                ROLLBACK;
            END IF;

            v_balance := (v_balance - p_tx_amount);

            UPDATE customer_accounts c SET balance = v_balance
            WHERE (c.id = p_account_id);
        END IF;
    END IF;

    INSERT INTO customer_transactions (account_id, type, amount, description)
        VALUES (p_account_id, p_tx_type, p_tx_amount, p_tx_description);

    o_credit_limit := v_credit_limit;
    o_balance := v_balance;

    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

GRANT INSERT ON customer_accounts TO javeiro;
GRANT UPDATE ON customer_accounts TO javeiro;
REVOKE DELETE ON customer_accounts FROM javeiro;

GRANT INSERT ON customer_transactions TO javeiro;
REVOKE UPDATE ON customer_transactions FROM javeiro;
REVOKE DELETE ON customer_transactions FROM javeiro;

INSERT INTO customer_accounts
       (id, credit_limit, balance)
VALUES (1, 1000 * 100, 0), -- R$1,000.00
       (2, 800 * 100, 0), -- R$8,000.00
       (3, 10000 * 100, 0), -- R$10,000.00
       (4, 100000 * 100, 0), -- R$100,000.00
       (5, 5000 * 100, 0); -- R$5,000.00