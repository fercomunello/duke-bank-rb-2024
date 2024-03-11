package com.github.bank.duke.business.control;

import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.business.entity.BankTransactionState;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.vertx.sql.Database;
import com.github.bank.duke.vertx.sql.Dialect;
import com.github.bank.duke.vertx.sql.function.UniTask;
import com.github.bank.duke.vertx.web.CachedEntry;
import com.github.bank.duke.vertx.web.validation.Validations;
import com.github.bank.duke.vertx.web.validation.constraints.NotNull;
import com.github.bank.duke.vertx.web.validation.constraints.PositiveInt;
import com.github.bank.duke.vertx.web.validation.constraints.TextLengthMax;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.impl.ArrayTuple;
import org.intellij.lang.annotations.Language;

import java.util.Optional;

import static com.github.bank.duke.business.control.BankTransactionResult.*;

public final class BankTransaction {

    @Language(value = Dialect.PSQL)
    private static final String PROCESS_CREDIT_TX_SQL =
        """
        WITH inserted_tx AS (
            INSERT INTO bank_transactions (account_id, type, amount, description)
            VALUES ($1, $2, $3, $4)
            RETURNING TRUE)
        UPDATE bank_accounts SET balance = balance + $3 WHERE (id = $1)
        RETURNING credit_limit, balance
        """;

    @Language(value = Dialect.PSQL)
    private static final String PROCESS_DEBIT_TX_SQL =
        """
            WITH account_state AS MATERIALIZED
                (SELECT c.credit_limit,
                       (c.balance - $3) AS new_balance,
                       CASE WHEN ((c.balance - $3) < -c.credit_limit)
                           THEN FALSE ELSE TRUE END AS debit_approved
                FROM bank_accounts c
                WHERE (c.id = $1)
                FOR UPDATE),
            updated_balance AS
                (UPDATE bank_accounts c SET balance = account_state.new_balance
                 FROM account_state WHERE (account_state.debit_approved) AND (c.id = $1)
                 RETURNING TRUE),
            performed_tx AS
                (INSERT INTO bank_transactions (account_id, type, amount, description)
                    SELECT $1, $2, $3, $4 FROM account_state s
                    JOIN updated_balance ON (TRUE)
                    WHERE (s.debit_approved)
                    RETURNING TRUE)
            SELECT s.credit_limit, 
                   s.new_balance, 
                   s.debit_approved
            FROM account_state s
            LEFT JOIN performed_tx ON (TRUE)
        """;

    private final TransactionType type;
    private final String description;
    private final long accountId;
    private final long amount;

    public BankTransaction(final TransactionType type,
                           final String description,
                           final long accountId,
                           final long amount) {
        this.type = type;
        this.description = description;
        this.accountId = accountId;
        this.amount = amount;
    }

    public static Uni<BankTransaction> of(final Long accountId, final JsonObject json) {
        final var type = TransactionType.of(json.getString(BankProtocol.TX_TYPE));
        final var description = json.getString(BankProtocol.TX_DESCRIPTION);
        final var amount = json.getNumber(BankProtocol.TX_AMOUNT);

        return new Validations(
            new NotNull(accountId, Entry.ACCOUNT_ID),
            new NotNull(type, Entry.TRANSACTION_TYPE, TransactionType.CONSTRAINT),
            new PositiveInt(amount, Entry.AMOUNT),
            new TextLengthMax(description, 10, Entry.DESCRIPTION, true))
            .eval(BankTransaction.class, () ->
                new BankTransaction(type, description, accountId, amount.longValue())
            );
    }

    public Uni<BankTransactionResult> perform() {
        var tuple = new ArrayTuple(4);
        tuple.addLong(this.accountId);
        tuple.addString(this.type.symbol());
        tuple.addLong(this.amount);
        tuple.addString(this.description);

        return withTransaction(conn ->
            switch (this.type) {
                case CREDIT -> conn.executeReturning(PROCESS_CREDIT_TX_SQL, tuple, out -> {
                    final var account = new BankAccount(
                        out.getLong(0),
                        out.getLong(1)
                    );
                    final var txState = new BankTransactionState(account, this.type, this.amount);
                    return new TransactionPerformed(txState);
                });
                case DEBIT -> conn.executeReturning(PROCESS_DEBIT_TX_SQL, tuple, out -> {
                    final var account = new BankAccount(
                        out.getLong(0),
                        out.getLong(1)
                    );
                    final var txState = new BankTransactionState(account, this.type, this.amount);
                    if (out.getBoolean(2)) {
                        return new TransactionPerformed(txState);
                    } else if (account.isCreditLimitExceeded()) {
                        return new AccountCreditExceeded(txState);
                    } else {
                        return new TransactionFailed(txState);
                    }
                });
            })
            .onFailure().recoverWithItem(_ -> Optional.of(new TransactionFailed()))
            .onItem().transform(Optional::get);

    }

    <T> Uni<T> withTransaction(final UniTask<T> task) {
        return Database.instance().withTransaction(task::execute);
    }

    public enum Entry implements CachedEntry {
        ACCOUNT_ID, TRANSACTION_TYPE, AMOUNT, DESCRIPTION
    }
}