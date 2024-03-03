package com.github.bank.duke.business.entity;

public record BankTransactionState(BankAccount account,
                                   TransactionType type,
                                   long amount) {}