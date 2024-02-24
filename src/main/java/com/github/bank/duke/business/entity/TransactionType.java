package com.github.bank.duke.business.entity;

import com.github.bank.duke.business.control.BankProtocol;

public enum TransactionType {

    CREDIT (  BankProtocol.CREDIT_SYMBOL ),
    DEBIT  (  BankProtocol.DEBIT_SYMBOL  );

    public final String symbol;

    TransactionType(final char symbol) {
        this.symbol = String.valueOf(symbol);
    }
}