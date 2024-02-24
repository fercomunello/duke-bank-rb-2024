package com.github.bank.duke.control;

import com.github.bank.duke.business.control.Result;
import com.github.bank.duke.business.entity.BankTransactionState;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.business.entity.BankAccount;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static com.github.bank.duke.business.entity.TransactionType.DEBIT;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
final class BankTransactionTest extends BankTransactionTestDouble {

    @Test
    @RunOnVertxContext
    @DisplayName("Perform Credit Transaction (C)")
    public void testPerformCreditTx(final UniAsserter asserter) {
        final long amount = (1_000L * 100);
        final var account = new BankAccount(1000 * 100, 0);

        asserter.assertThat(
            () -> createAccount(account).chain(accountId ->
                performTransaction(TransactionType.CREDIT, "#Credit TX", accountId, amount)
            ),
            result -> {
                assertInstanceOf(Result.Success.class, result);

                final BankTransactionState transactionState = result.value();
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
    public void testPerformDebitTx(final UniAsserter asserter) {
        final long firstAmount = (1_700L * 100),
                   secondAmount = (60L * 100);

        final var account = new BankAccount(1000 * 100, 0);
        final var accountIdReference = new AtomicReference<Long>();

        asserter.assertThat(
            () -> createAccount(account).chain(accountId -> {
                accountIdReference.set(accountId);
                return performTransaction(TransactionType.CREDIT, accountId, firstAmount);
            }),
            result -> {
                assertInstanceOf(Result.Success.class, result);

                final BankTransactionState transactionState = result.value();
                assertEquals(TransactionType.CREDIT, transactionState.type());
                assertEquals(firstAmount, transactionState.amount());

                final BankAccount accountState = transactionState.account();
                assertEquals(firstAmount, accountState.balance());
                assertEquals(account.creditLimit(), accountState.creditLimit());
                assertFalse(accountState.isCreditLimitExceeded());
            }
        );

        asserter.assertThat(
            () -> performTransaction(TransactionType.DEBIT, accountIdReference.get(), secondAmount),
            result -> {
                assertInstanceOf(Result.Success.class, result);

                final BankTransactionState transactionState = result.value();
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
    @DisplayName("Reject Debit (D) Transactions that exceeds account credit limit")
    public void testCreditLimitExceeded(final UniAsserter asserter) {
        final long amount = (2000 * 100);
        final var account = new BankAccount(1000 * 100, 0);

        asserter.assertThat(
            () -> createAccount(account).chain(accountId ->
                performTransaction(DEBIT, accountId, amount)
            ),
            result -> {
                assertInstanceOf(Result.Failure.class, result);

                final BankTransactionState transactionState = result.value();
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