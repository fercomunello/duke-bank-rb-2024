/* =================================================================
# PL/PGSQL - Tests
==================================================================== */

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

