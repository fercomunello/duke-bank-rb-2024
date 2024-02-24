package com.github.bank.duke.business.control.result;

import com.github.bank.duke.business.entity.BankTransactionState;

public sealed interface BankTransactionResult extends Result<BankTransactionState> {

    record TransactionPerformed(BankTransactionState entity) implements BankTransactionResult {}
    record AccountCreditExceeded(BankTransactionState entity) implements BankTransactionResult {}
    record Failure() implements BankTransactionResult {}
}