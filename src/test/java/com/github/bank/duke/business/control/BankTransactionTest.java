package com.github.bank.duke.business.control;

import com.github.bank.duke.business.Bank;
import com.github.bank.duke.business.BankTest;
import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.business.entity.BankTransactionState;
import com.github.bank.duke.business.entity.TransactionType;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
final class BankTransactionTest extends BankTest {

    @Inject
    Bank bank;

    @Test
    @RunOnVertxContext
    @DisplayName("Perform Credit Transaction (C)")
    void testPerformCreditTransaction(final UniAsserter asserter) {
        final long amount = (1_000L * 100);
        final var account = new BankAccount(0, 0);

        asserter.assertThat(
            () -> this.bank.createAccount(account).chain(accountId ->
                this.bank.performTransaction(TransactionType.CREDIT, accountId, amount)
            ),
            tx -> {
                assertInstanceOf(BankTransactionResult.TransactionPerformed.class, tx);

                final BankTransactionState transactionState = tx.transactionState();
                assertEquals(TransactionType.CREDIT, transactionState.type());
                assertEquals(amount, transactionState.amount());

                final BankAccount accountState = transactionState.account();
                assertEquals(amount, accountState.balance());
                assertEquals(account.creditLimit(), accountState.creditLimit());
            }
        );
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Perform Credit (C) + Debit (D) Transaction")
    void testPerformCreditPlusDebitTransactions(final UniAsserter asserter) {
        final long firstAmount = (1_700L * 100),
                   secondAmount = (60L * 100);

        final var account = new BankAccount(1000 * 100, 0);
        final var accountIdReference = new AtomicReference<Long>();

        asserter.assertThat(
            () -> this.bank.createAccount(account).chain(accountId -> {
                accountIdReference.set(accountId);
                return this.bank.performTransaction(TransactionType.CREDIT, accountId, firstAmount);
            }),
            tx -> {
                assertInstanceOf(BankTransactionResult.TransactionPerformed.class, tx);

                final BankTransactionState transactionState = tx.transactionState();
                assertEquals(TransactionType.CREDIT, transactionState.type());
                assertEquals(firstAmount, transactionState.amount());

                final BankAccount accountState = transactionState.account();
                assertEquals(firstAmount, accountState.balance());
                assertEquals(account.creditLimit(), accountState.creditLimit());
                assertFalse(accountState.isCreditLimitExceeded());
            }
        );

        asserter.assertThat(() -> this.bank.performTransaction(
                TransactionType.DEBIT, accountIdReference.get(), secondAmount),
            tx -> {
                assertInstanceOf(BankTransactionResult.TransactionPerformed.class, tx);

                final BankTransactionState transactionState = tx.transactionState();
                assertEquals(TransactionType.DEBIT, transactionState.type());
                assertEquals(secondAmount, transactionState.amount());

                final BankAccount accountState = transactionState.account();
                assertEquals(1_640L * 100, accountState.balance());
                assertEquals(account.creditLimit(), accountState.creditLimit());
                assertFalse(accountState.isCreditLimitExceeded());
            }
        );
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Reject Debit (D) Transaction that exceeds account credit limit")
    void testCreditLimitExceeded(final UniAsserter asserter) {
        final long amount = (2000 * 100);
        final var account = new BankAccount(1000 * 100, 0);

        asserter.assertThat(
            () -> this.bank.createAccount(account).chain(accountId ->
                this.bank.performTransaction(TransactionType.DEBIT, accountId, amount)
            ),
            tx -> {
                assertInstanceOf(BankTransactionResult.AccountCreditExceeded.class, tx);

                final BankTransactionState transactionState = tx.transactionState();
                assertEquals(TransactionType.DEBIT, transactionState.type());
                assertEquals(amount, transactionState.amount());

                final BankAccount accountState = transactionState.account();

                assertEquals(-amount, accountState.balance());
                assertEquals(account.creditLimit(), accountState.creditLimit());
                assertTrue(accountState.isCreditLimitExceeded());
            }
        );
    }
}