package com.github.bank.duke.business.control;

import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.vertx.sql.Database;
import com.github.bank.duke.vertx.sql.Dialect;
import io.smallrye.mutiny.Uni;
import io.vertx.sqlclient.Tuple;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.intellij.lang.annotations.Language;

import java.util.Optional;

@Singleton
public final class Bank {

    @Inject
    Database database;

    public Uni<Long> createAccount(final BankAccount account) {
        return this.database.withTransaction(connection -> {
            final @Language(value = Dialect.SQL) var sql =
                "INSERT INTO bank_accounts (credit_limit, balance) VALUES ($1, $2) RETURNING id";

            final var tuple = Tuple.of(account.creditLimit(), account.balance());

            return connection.executeReturning(sql, tuple, row -> row.getLong(0))
                .onItem().transform(Optional::get);
        });
    }
}
