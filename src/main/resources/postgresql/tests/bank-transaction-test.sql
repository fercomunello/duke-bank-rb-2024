/* =================================================================
# PL/PGSQL - Tests
==================================================================== */

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
BEGIN
    IF (p_account_id IS NULL OR p_type IS NULL OR p_amount <= 0) THEN
        RAISE check_violation USING MESSAGE =
                'TX must be associated with an account, it must has a type (C-Credit | D-Debit) and an amount.',
            HINT = 'Check the validation steps on the application side';
    END IF;

    IF (p_type = 'c'::TXTYPE) THEN
        UPDATE bank_accounts SET balance = (balance + p_amount) WHERE (id = p_account_id)
        RETURNING credit_limit, balance INTO v_credit_limit, v_balance;

        o_tx_performed := TRUE;
    ELSIF (p_type = 'd'::TXTYPE) THEN
        SELECT c.credit_limit,
               c.balance
        INTO v_credit_limit,
            v_balance
        FROM bank_accounts c
        WHERE (c.id = p_account_id)
            FOR UPDATE;

        IF (FOUND) THEN
            v_balance := (v_balance - p_amount);
            IF (v_balance < -v_credit_limit) THEN
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

DELETE FROM bank_transactions WHERE account_id IN (1000, 1001);
DELETE FROM bank_accounts WHERE id IN (1000, 1001);

INSERT INTO bank_accounts (id, credit_limit, balance)
VALUES (1000, 1000 * 100, 0), (1001, 5000 * 100, 0);

DO $$ -- TEST: Should deposit the amount into the account.
DECLARE
    tx_performed BOOL;
    credit_limit BIGINT;
    balance BIGINT;
    expected BOOL;
BEGIN
    SELECT * FROM process_bank_transaction(
            1000,
            'c'::TXTYPE,
            (200 * 100)::INT,
            substr(gen_random_uuid()::VARCHAR, 1, 4)::VARCHAR)
    INTO tx_performed, credit_limit, balance;

    ROLLBACK;

    expected := tx_performed = TRUE
        AND credit_limit = 100000
        AND balance = 20000;

    IF (NOT expected) THEN
        RAISE EXCEPTION '[X] Account Deposit Test FAILED.';
    ELSE
        RAISE NOTICE '[OK] Account Deposit Test PASSED.';
    END IF;
END; $$;

DO $$ -- TEST: Should debit/withdraw the entire amount from the account.
DECLARE
    tx_performed BOOL;
    credit_limit BIGINT;
    balance BIGINT;
    expected BOOL;
BEGIN
    SELECT * FROM process_bank_transaction(
            1001,
            'd'::TXTYPE,
            (5000 * 100)::INT,
            substr(gen_random_uuid()::VARCHAR, 1, 4)::VARCHAR)
    INTO tx_performed, credit_limit, balance;

    ROLLBACK;

    expected := tx_performed = TRUE
        AND credit_limit = 500000
        AND balance = -500000;

    IF (NOT expected) THEN
        RAISE EXCEPTION '[X] Account Withdrawal Test FAILED.';
    ELSE
        RAISE NOTICE '[OK] Account Withdrawal Test PASSED.';
    END IF;
END; $$;

DO $$ -- TEST: Should reject and rollback the debit TX when the credit creditLimit is exceeded.
DECLARE
    tx_performed BOOL;
    credit_limit BIGINT;
    balance BIGINT;
    expected BOOL;
BEGIN
    PERFORM process_bank_transaction(
            1001,
            'd'::TXTYPE,
            (5000 * 100)::INT,
            substr(gen_random_uuid()::VARCHAR, 1, 4)::VARCHAR);

    SELECT o_tx_performed,
           o_credit_limit,
           o_balance
    FROM process_bank_transaction(
        1001,
        'd'::TXTYPE,
        (35 * 100)::INT,
        substr(gen_random_uuid()::VARCHAR, 1, 4)::VARCHAR)
    INTO tx_performed, credit_limit, balance;

    ROLLBACK;

    expected := tx_performed = FALSE
                AND credit_limit = 500000
                AND balance = -503500;

    IF (NOT expected) THEN
        RAISE EXCEPTION '[X] Credit Limit Check Test FAILED';
    ELSE
        RAISE NOTICE '[OK] Credit Limit Check Test PASSED';
    END IF;
END; $$;

DO $$
DECLARE
    expected BOOL := FALSE;
BEGIN
    BEGIN
        -- Should retrieve a check violation because account id is NULL:
        PERFORM process_bank_transaction(
                NULL,
                'd'::TXTYPE,
                1::INT,
                substr(gen_random_uuid()::VARCHAR, 1, 4)::VARCHAR);

        -- Should throw a check violation when the given type is NULL:
        PERFORM process_bank_transaction(
                1001,
                'c'::TXTYPE,
                0::INT,
                substr(gen_random_uuid()::VARCHAR, 1, 4)::VARCHAR);
    EXCEPTION
        WHEN check_violation THEN
            expected := TRUE;
    END;

    IF NOT expected THEN
        RAISE EXCEPTION '[X] Check Violation Test FAILED.';
    ELSE
        RAISE NOTICE '[OK] Check Violation Test PASSED.';
    END IF;
END $$;

DELETE FROM bank_transactions WHERE account_id IN (1000, 1001);
DELETE FROM bank_accounts WHERE id IN (1000, 1001);

