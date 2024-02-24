package com.github.bank.duke.control;

import com.github.bank.duke.BankSchema;
import com.github.bank.duke.business.control.BankTransaction;
import com.github.bank.duke.business.control.result.BankTransactionResult;
import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.vertx.sql.Database;
import com.github.bank.duke.vertx.sql.Dialect;
import io.smallrye.mutiny.Uni;
import io.vertx.sqlclient.Tuple;
import jakarta.inject.Inject;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;

import java.util.Optional;

abstract class BankTransactionTestDouble {

    @Inject
    Database database;

    @Inject
    BankSchema bankSchema;

    @AfterEach
    void afterEach() {
        this.bankSchema.regenerate();
    }

    protected Uni<BankTransactionResult> performTransaction(
        final TransactionType type,
        final Long accountId,
        final long amount)
    {
        return performTransaction(type, null, accountId, amount);
    }

    protected Uni<BankTransactionResult> performTransaction(
        final TransactionType type,
        final @Nullable String description,
        final Long accountId,
        final long amount)
    {
        return new BankTransaction(accountId, type, description, amount).execute();
    }

    protected Uni<Long> createAccount(final BankAccount account) {
        return this.database.withTransaction(connection -> {
            final @Language(value = Dialect.SQL) var sql =
                "INSERT INTO bank_accounts (credit_limit, balance) VALUES ($1, $2) RETURNING id";

            final var tuple = Tuple.of(account.creditLimit(), account.balance());

            return connection.executeReturning(sql, tuple, row -> row.getLong(0))
                .onItem().transform(Optional::get);
        });
    }
}
