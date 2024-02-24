package com.github.bank.duke.business.control;

import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.business.entity.BankTransactionState;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.vertx.sql.Dialect;
import com.github.bank.duke.vertx.sql.StoredFunction;
import io.smallrye.mutiny.Uni;
import io.vertx.sqlclient.impl.ArrayTuple;
import org.intellij.lang.annotations.Language;

import java.util.Optional;

public final class BankTransaction implements StoredFunction<BankTransactionState> {

    @Language(value = Dialect.PSQL)
    private static final String PROCESS_TX_SQL =
        """
        SELECT o_tx_performed, 
               o_credit_limit,
               o_balance
        FROM process_bank_transaction(
            $1::BIGINT, 
            $2::TXTYPE, 
            $3::INT, 
            $4::VARCHAR)""";

    private static final int POS_TX_PERFORMED = 0,
                             POS_CREDIT_LIMIT = 1, POS_BALANCE = 2;

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

    @Override
    public Uni<Result<BankTransactionState>> execute() {
        final var tuple = new ArrayTuple(4);
        tuple.addLong(this.accountId);
        tuple.addString(this.type.symbol);
        tuple.addLong(this.amount);
        tuple.addString(this.description);

        return withTransaction(conn ->
            conn.executeReturning(PROCESS_TX_SQL, tuple, out -> {
                final var account = new BankAccount(
                    out.getLong(POS_CREDIT_LIMIT),
                    out.getLong(POS_BALANCE)
                );

                final var transactionState = new BankTransactionState(account, this.type, this.amount);

                if (out.getBoolean(POS_TX_PERFORMED)) {
                    final boolean creditLimitExceeded = account.isCreditLimitExceeded();
                    if (!creditLimitExceeded) {
                        return new Result.Success<>(transactionState);
                    } else {
                        return new Result.Failure<>(transactionState, () -> STR."""
                            Debit transaction rejected, insufficient
                            credit limit for account \{accountId}."""
                        );
                    }
                }
                return new Result.Failure<>(transactionState);
            }).onItem()
              .transform(Optional::get)
        );
    }
}
