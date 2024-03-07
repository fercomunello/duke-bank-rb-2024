package com.github.bank.duke.business.control;

import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.business.entity.BankTransactionState;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.vertx.sql.Dialect;
import com.github.bank.duke.vertx.sql.StoredFunction;
import com.github.bank.duke.vertx.web.CachedEntry;
import com.github.bank.duke.vertx.web.validation.constraints.NotNull;
import com.github.bank.duke.vertx.web.validation.constraints.PositiveInt;
import com.github.bank.duke.vertx.web.validation.constraints.TextLengthMax;
import com.github.bank.duke.vertx.web.validation.Validations;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.impl.ArrayTuple;
import org.intellij.lang.annotations.Language;

import java.util.Optional;

import static com.github.bank.duke.business.control.BankTransactionResult.*;

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
            $3::BIGINT, 
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

    @Override
    public Uni<BankTransactionResult> perform() {
        final var tuple = new ArrayTuple(4);
        tuple.addLong(this.accountId);
        tuple.addString(this.type.symbol());
        tuple.addLong(this.amount);
        tuple.addString(this.description);

        return withTransaction(conn ->
            conn.executeReturning(PROCESS_TX_SQL, tuple, out -> {
                final var account = new BankAccount(
                    out.getLong(POS_CREDIT_LIMIT),
                    out.getLong(POS_BALANCE)
                );
                final var txState = new BankTransactionState(account, this.type, this.amount);
                if (out.getBoolean(POS_TX_PERFORMED)) {
                    return new TransactionPerformed(txState);
                } else if (account.isCreditLimitExceeded()) {
                    return new AccountCreditExceeded(txState);
                } else {
                    return new TransactionFailed(txState);
                }
            }).onItem().transform(Optional::get)
        );
    }

    public enum Entry implements CachedEntry {
        ACCOUNT_ID, TRANSACTION_TYPE, AMOUNT, DESCRIPTION
    }
}