package com.github.bank.duke.business.control;

import com.github.bank.duke.business.entity.BankTransactionState;

public sealed interface BankTransactionResult {

    record TransactionPerformed(BankTransactionState transactionState) implements BankTransactionResult {}
    record AccountCreditExceeded(BankTransactionState transactionState) implements BankTransactionResult {}
    record TransactionFailure() implements BankTransactionResult {}

    default BankTransactionState transactionState() { return null; }
}