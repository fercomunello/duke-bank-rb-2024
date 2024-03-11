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
import java.util.function.BiFunction;

import static com.github.bank.duke.business.control.BankProtocol.*;

@Singleton
public final class BankStatement {

    @Language(value = Dialect.SQL)
    private static final String ACCOUNT_STATE_QUERY = """
        SELECT a.balance,
               a.credit_limit
        FROM bank_accounts a
        WHERE (a.id = $1)""";

    @Language(value = Dialect.SQL)
    private static final String LAST_TXS_QUERY = """
        SELECT tx.amount,
               tx.type,
               tx.description,
               tx.issued_at
         FROM bank_transactions tx
         WHERE (tx.account_id = $1)
         ORDER BY tx.issued_at DESC
         LIMIT 10""";

    @Inject
    Database database;

    public Uni<JsonObject> aggregateBankStatements(final Long accountId) {
        return this.database.withConnection(conn -> {
            final Uni<Object> accountStateUni = conn.select(ACCOUNT_STATE_QUERY, Tuple.of(accountId))
                .map(iterator -> {
                    if (!iterator.hasNext()) return null;
                    final Row row = iterator.next();
                    return new JsonObject(new LinkedHashMap<>(3))
                        .put(TOTAL, row.getLong(0))
                        .put(STATEMENT_TIME, DATE_TIME_FORMATTER.format(OffsetDateTime.now()))
                        .put(CREDIT_LIMIT, row.getLong(1));
                });

            final Uni<Object> lastTransactionsUni = conn.select(LAST_TXS_QUERY, Tuple.of(accountId))
                .map(iterator -> {
                    if (!iterator.hasNext()) return new JsonArray();

                    final var lastTransactions = new JsonArray();
                    transactionsAggregator:
                    {
                        Row row;
                        for (;;) {
                            if (!iterator.hasNext()) {
                                break transactionsAggregator;
                            }
                            row = iterator.next();
                            lastTransactions.add(
                                new JsonObject(new LinkedHashMap<>(4))
                                    .put(TX_AMOUNT, row.getLong(0))
                                    .put(TX_TYPE, row.getString(1))
                                    .put(TX_DESCRIPTION, row.getString(2))
                                    .put(TX_ISSUED_AT_TIME, DATE_TIME_FORMATTER.format(row.getOffsetDateTime(3)))
                            );
                        }
                    }

                    return lastTransactions;
                });

                return Uni.combine().all().unis(accountStateUni, lastTransactionsUni)
                    .with(new BiFunction<>() {
                        @Override
                        public JsonObject apply(final Object accountState, final Object lastTransactionsArray) {
                            if (accountState == null) return EMPTY_JSON;
                            return JsonObject.of(BALANCE, accountState, LAST_TXS, lastTransactionsArray);
                        }
                    });
            }
        );
    }
}
