package com.github.bank.duke.business.entity;

import com.github.bank.duke.vertx.web.i18n.ValidationMessages;
import com.github.bank.duke.vertx.web.validation.failure.Constraint;
import org.jetbrains.annotations.Nullable;

public enum TransactionType {

    CREDIT ('C', "Deposit"),
    DEBIT  ('D', "Withdrawal");

    private final String symbol;
    private final String description;

    TransactionType(final char symbol, final String description) {
        this.symbol = String.valueOf(symbol).toLowerCase();
        this.description = description;
    }

    public static final Constraint CONSTRAINT =
        () -> ValidationMessages.instance().get("tx.type.is.invalid")
                .formatted(CREDIT.symbol.toUpperCase(), DEBIT.symbol.toUpperCase());

    @Nullable
    public static TransactionType of(final String symbol) {
        if (CREDIT.symbol().equalsIgnoreCase(symbol)) return CREDIT;
        else if (DEBIT.symbol().equalsIgnoreCase(symbol)) return DEBIT;
        return null;
    }

    public String symbol() {
        return this.symbol;
    }

    public String description() {
        return this.description;
    }
}