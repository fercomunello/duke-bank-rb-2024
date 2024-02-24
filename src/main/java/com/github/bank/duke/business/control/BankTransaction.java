package com.github.bank.duke.business.control;

import com.github.bank.duke.business.control.result.BankTransactionResult;
import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.business.entity.BankTransactionState;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.vertx.sql.Dialect;
import com.github.bank.duke.vertx.sql.StoredFunction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.impl.ArrayTuple;
import org.intellij.lang.annotations.Language;

import java.util.Optional;

public final class BankTransaction implements StoredFunction<BankTransactionResult> {

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
    private final Long accountId;
    private final Long amount;

    public BankTransaction(final Long accountId, final JsonObject payload) {
        this(accountId, TransactionType.of(payload.getString(BankProtocol.TX_TYPE)),
            payload.getString(BankProtocol.DESCRIPTION),
            payload.getLong(BankProtocol.AMOUNT)
        );
    }

    public BankTransaction(final Long accountId,
                           final TransactionType type,
                           final String description,
                           final Long amount) {
        this.type = type;
        this.description = description;
        this.accountId = accountId;
        this.amount = amount;
    }

    @Override
    public Uni<BankTransactionResult> execute() {
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
                    return new BankTransactionResult.TransactionPerformed(transactionState);
                } else if (account.isCreditLimitExceeded()) {
                    return new BankTransactionResult.AccountCreditExceeded(transactionState);
                } else {
                    return new BankTransactionResult.Failure();
                }
            }).onItem().transform(Optional::get)
        );
    }
}
