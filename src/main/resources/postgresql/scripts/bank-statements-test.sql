DO $$ -- 30625(C) + 30625 (D) = 61,250 rows
DECLARE
    tx_type TXTYPE := 'c';
    commit_window SMALLINT := 50;
    rows INT := 30625;
    i INT := 1;
    j INT := 1;
BEGIN
    WHILE i <= rows LOOP
        IF (i = rows AND tx_type = 'c') THEN
            i := 1;
            tx_type := 'd';
        END IF;
        PERFORM process_bank_transaction(
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

SELECT * FROM bank_transactions;

-- [Summary] - Bank Statements => Plans analysis tool: tatiyants.com/pev/#/plans/new
-- * Largest node (rows): 11 rows | Costliest node: 41,190.41
EXPLAIN (ANALYZE, COSTS, VERBOSE, FORMAT JSON, BUFFERS)
WITH account_totals AS (
    SELECT c.balance,
           c.credit_limit
    FROM bank_accounts c
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
 FROM bank_transactions tx
 WHERE (tx.account_id = :acc_id) -- BITMAP INDEX SCAN
 ORDER BY tx.issued_at DESC -- INDEX SCAN
 LIMIT 10);

-- Enables query execution metrics
CREATE EXTENSION pg_stat_statements;

-- Fetch recent metrics
SELECT
    query,
    total_exec_time,
    rows,
    calls,
    shared_blks_read,
    shared_blks_hit,
    blk_read_time,
    blk_write_time,
    shared_blks_written
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 500;

-- Reset metrics
SELECT pg_stat_statements_reset();