package com.github.bank.duke.business.entity;

import com.github.bank.duke.business.control.BankProtocol;
import com.github.bank.duke.vertx.web.Media;
import io.vertx.core.json.JsonObject;

import java.util.LinkedHashMap;

public final class BankAccount implements Media<JsonObject> {

    private final long creditLimit;
    private final long balance;

    private JsonObject json;

    public BankAccount(final long creditLimit, final long balance) {
        this.creditLimit = creditLimit;
        this.balance = balance;
    }

    @Override
    public JsonObject serialize() {
        return this.json == null ?
            this.json = (new JsonObject(new LinkedHashMap<>(2))
                .put(BankProtocol.CREDIT_LIMIT, this.creditLimit)
                .put(BankProtocol.BALANCE, this.balance))
            : this.json;
    }

    public boolean isCreditLimitExceeded() {
        return this.balance < -this.creditLimit;
    }

    public long creditLimit() {
        return this.creditLimit;
    }

    public long balance() {
        return this.balance;
    }
}