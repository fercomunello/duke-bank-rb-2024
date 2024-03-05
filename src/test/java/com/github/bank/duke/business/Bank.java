package com.github.bank.duke.business;

import com.github.bank.duke.business.control.BankTransaction;
import com.github.bank.duke.business.control.BankTransactionResult;
import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.vertx.sql.Database;
import com.github.bank.duke.vertx.sql.Dialect;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.sqlclient.Tuple;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.intellij.lang.annotations.Language;

import java.util.Optional;
import java.util.Random;

@Singleton
public final class Bank {

    @Inject
    Database database;

    public Uni<Long> createAccount(final long creditLimit, final long balance) {
        return createAccount(new BankAccount(creditLimit, balance));
    }

    public Uni<Long> createAccount(final BankAccount account) {
        return this.database.withTransaction(connection -> {
            final @Language(value = Dialect.SQL) var sql =
                "INSERT INTO bank_accounts (credit_limit, balance) VALUES ($1, $2) RETURNING id";

            final var tuple = Tuple.of(account.creditLimit(), account.balance());

            return connection.executeReturning(sql, tuple, row -> row.getLong(0))
                .onItem().transform(Optional::get);
        });
    }

    public void populateTransactions(final long accountId, final int iterations) {
        Multi.createFrom().range(1, iterations + 1)
            .onItem().transformToUni(sequence -> {
                final var random = new Random();
                final var type = sequence % 2 == 0 ? TransactionType.CREDIT : TransactionType.DEBIT;
                final var description = STR."# TX \{sequence}";

                return this.performTransaction(type, accountId, type == TransactionType.CREDIT
                    ? (random.nextLong((10_000 * 100) - 100 + 1) + 100)
                    : (random.nextLong((1_500 * 100) - 100 + 1) + 100), description);
            }).concatenate()
            .onItem().ignoreAsUni()
            .await().indefinitely();
    }

    public Uni<BankTransactionResult> performTransaction(final TransactionType type, final Long accountId,
                                                         final long amount, final String ... args) {
        final var description = (args.length > 0 ? String.join(" ", args) : null);
        return new BankTransaction(type, description, accountId, amount).perform();
    }
}
