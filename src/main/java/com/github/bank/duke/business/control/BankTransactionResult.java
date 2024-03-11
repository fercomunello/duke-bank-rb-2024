package com.github.bank.duke.business.control;

import com.github.bank.duke.business.entity.BankTransactionState;

public sealed interface BankTransactionResult {

    record TransactionPerformed(BankTransactionState transactionState) implements BankTransactionResult {}
    record AccountCreditExceeded(BankTransactionState transactionState) implements BankTransactionResult {}
    record TransactionFailed(BankTransactionState transactionState) implements BankTransactionResult {
        public TransactionFailed() {
            this(null);
        }
    }

    BankTransactionState transactionState();
}