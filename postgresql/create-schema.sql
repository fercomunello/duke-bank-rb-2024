DROP SCHEMA IF EXISTS api CASCADE;
CREATE SCHEMA api;

SET search_path TO api;
ALTER DATABASE rinhadb SET search_path TO api;

SET TIME ZONE 'UTC';

-- DO $$
--     BEGIN
--         INSERT INTO customers (name, credit_limit)
--         VALUES
--             ('o barato sai caro', 1000 * 100),
--             ('zan corp ltda', 800 * 100),
--             ('les cruders', 10000 * 100),
--             ('padaria joia de cocaia', 100000 * 100),
--             ('kid mais', 5000 * 100);
--     END; $$