package com.github.bank.duke.business.control;

import com.github.bank.duke.vertx.sql.Database;
import com.github.bank.duke.vertx.sql.Dialect;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.intellij.lang.annotations.Language;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;

import static com.github.bank.duke.business.control.BankProtocol.*;

@Singleton
public final class BankStatement {

    @Language(value = Dialect.SQL)
    private static final String BANK_STATEMENT_QUERY = """
        WITH account_totals AS (
            SELECT a.balance,
                   a.credit_limit
            FROM bank_accounts a
            WHERE (a.id = $1)
        )
        (SELECT t.balance,
                t.credit_limit,
                NULL AS amount,
                NULL AS type,
                NULL AS description,
                NULL AS issued_at
         FROM account_totals t)
        UNION ALL
        (SELECT
             NULL,
             NULL,
             tx.amount,
             tx.type,
             tx.description,
             tx.issued_at
         FROM bank_transactions tx
         WHERE (tx.account_id = $2)
         ORDER BY tx.issued_at DESC
         LIMIT 10)""";

    @Inject
    Database database;

    public Uni<JsonObject> aggregateBankStatements(final Long accountId) {
        return this.database.withConnection(conn ->
            conn.select(BANK_STATEMENT_QUERY, Tuple.of(accountId, accountId))).map(iterator -> {
                if (!iterator.hasNext()) return EMPTY_JSON;

                Row row = iterator.next();
                final JsonObject accountState = new JsonObject(new LinkedHashMap<>(3))
                    .put(TOTAL, row.getLong(0))
                    .put(STATEMENT_TIME, DATE_TIME_FORMATTER.format(OffsetDateTime.now()))
                    .put(CREDIT_LIMIT, row.getLong(1));

                final var lastTransactions = new JsonArray();
                transactionsAggregator: {
                    for (;;) {
                        if (!iterator.hasNext()) {
                            break transactionsAggregator;
                        }
                        row = iterator.next();
                        lastTransactions.add(
                            new JsonObject(new LinkedHashMap<>(4))
                                .put(TX_AMOUNT, row.getLong(2))
                                .put(TX_TYPE, row.getString(3))
                                .put(TX_DESCRIPTION, row.getString(4))
                                .put(TX_ISSUED_AT_TIME, DATE_TIME_FORMATTER.format(row.getOffsetDateTime(5)))
                        );
                    }
                }

                return JsonObject.of(BALANCE, accountState,
                                     LAST_TXS, lastTransactions);
            });
    }
}
