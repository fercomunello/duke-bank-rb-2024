package com.github.bank.duke.control;

import com.github.bank.duke.BankSchema;
import com.github.bank.duke.business.control.BankTransaction;
import com.github.bank.duke.business.entity.BankTransactionState;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.bank.duke.business.entity.TransactionType.CREDIT;
import static com.github.bank.duke.business.entity.TransactionType.DEBIT;
import static com.github.bank.duke.entity.data.BankAccounts.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class BankTransactionTest {

    @Inject
    BankSchema bankSchema;

    @AfterEach
    void afterEach() {
        this.bankSchema.regenerate();
    }

    @Test
    @DisplayName("Perform Credit Transaction (C)")
    @RunOnVertxContext
    public void testPerformCreditTx(final UniAsserter asserter) {
        final long amount = (1_000L * 100);

        final Uni<BankTransactionState> performBankTransaction =
            new BankTransaction(CREDIT, CREDIT_TX_REASON, ACCOUNT.id(), amount)
                .execute();

        asserter.assertThat(() -> performBankTransaction, txState -> {
            assertEquals(amount, txState.balance());
            assertEquals(ACCOUNT.limit(), txState.creditLimit());
        });
    }

    @Test
    @DisplayName("Perform Credit (C) + Debit (D) Transaction")
    @RunOnVertxContext
    public void testPerformDebitTx(final UniAsserter asserter) {
        final long amount1 = (1_700L * 100),
                   amount2 = (60L * 100);

        asserter.assertThat(
            () -> new BankTransaction(CREDIT, CREDIT_TX_REASON, ACCOUNT.id(), amount1).execute(),
            txState -> {
                assertEquals(amount1, txState.balance());
                assertEquals(ACCOUNT.limit(), txState.creditLimit());
            }
        );
        asserter.assertThat(
            () -> new BankTransaction(DEBIT, DEBIT_TX_REASON, ACCOUNT.id(), amount2).execute(),
            txState -> {
                assertEquals(1_640L * 100, txState.balance());
                assertEquals(ACCOUNT.limit(), txState.creditLimit());
            }
        );
    }
}
