DO $$ -- Generate ~ 61k rows...
DECLARE
    tx_type TXTYPE := 'c';
    commit_window SMALLINT := 50;
    rows INT := 11250; -- 11250(C) + 11250 (D) = 61,250 rows
    i INT := 1;
    j INT := 1;
BEGIN
    WHILE i <= rows LOOP
        IF (i = rows AND tx_type = 'c') THEN
            i := 1;
            tx_type := 'd';
        END IF;
        PERFORM process_customer_tx(
                floor((random() * 5) + 1)::BIGINT,
                tx_type::TXTYPE,
                floor((random() * (CASE WHEN tx_type = 'c' THEN 750 ELSE 350 END)) + 15)::INT,
                substr(gen_random_uuid()::VARCHAR, 1, 4)::VARCHAR);
        i := i + 1;
        IF (j = commit_window) THEN
            COMMIT;
            j := 1;
        END IF;
        j := j + 1;
    END LOOP;
    COMMIT;
END $$;
SELECT * FROM customer_transactions;

-- [Summary] - Bank Statements
EXPLAIN (ANALYZE, COSTS, VERBOSE, FORMAT JSON, BUFFERS)
WITH account_totals AS (
    SELECT c.balance,
           c.credit_limit
    FROM customer_accounts c
    WHERE (c.id = :acc_id) -- BITMAP INDEX SCAN
)
(SELECT t.balance,
        t.credit_limit,
        NULL AS type,
        NULL AS amount,
        NULL AS description,
        NULL AS issued_at
 FROM account_totals t)
UNION ALL
(SELECT
     NULL,
     NULL,
     tx.type,
     tx.amount,
     tx.description,
     tx.issued_at
 FROM customer_transactions tx
 WHERE (tx.account_id = :acc_id) -- BITMAP INDEX SCAN
 ORDER BY tx.issued_at DESC -- INDEX SCAN
 LIMIT 10);