package com.github.bank.duke.business.entity;

import com.github.bank.duke.business.entity.naming.RBF;

public enum TransactionType {

    CREDIT (  RBF.CREDIT_SYMBOL ),
    DEBIT  (  RBF.DEBIT_SYMBOL  );

    public final String symbol;

    TransactionType(final char symbol) {
        this.symbol = String.valueOf(symbol);
    }
}
