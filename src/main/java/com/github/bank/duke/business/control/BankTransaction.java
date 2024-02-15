package com.github.bank.duke.business.control;

import com.github.bank.duke.business.entity.BankTransactionState;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.vertx.sql.Dialect;
import com.github.bank.duke.vertx.sql.StoredFunction;
import io.smallrye.mutiny.Uni;
import io.vertx.sqlclient.impl.ArrayTuple;
import org.intellij.lang.annotations.Language;

import java.util.Optional;

public final class BankTransaction extends StoredFunction<BankTransactionState> {

    @Language(value = Dialect.PSQL)
    private static final String STMT =
        """
        SELECT
            o_credit_limit,
            o_balance
        FROM process_bank_transaction(
            $1::BIGINT, 
            $2::TXTYPE, 
            $3::INT, 
            $4::VARCHAR)""";

    private static final int POS_CREDIT_LIMIT = 0, POS_BALANCE = 1;

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
    public Uni<BankTransactionState> execute() {
        final var tuple = new ArrayTuple(4);
        tuple.addLong(this.accountId);
        tuple.addString(this.type.symbol);
        tuple.addLong(this.amount);
        tuple.addString(this.description);

        return withTransaction((connection, pool) ->
            pool.executeQuery(connection, STMT, tuple, out ->
                new BankTransactionState(
                    out.getLong(POS_CREDIT_LIMIT),
                    out.getLong(POS_BALANCE)
                )
            ).map(Optional::get)
        );
    }
}
