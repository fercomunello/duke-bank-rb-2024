package com.github.bank.duke.business.entity;

import com.github.bank.duke.business.control.BankProtocol;
import com.github.bank.duke.vertx.web.Media;
import io.vertx.core.json.JsonObject;

import java.util.LinkedHashMap;

public record BankTransactionState(BankAccount account,
                                   TransactionType type,
                                   long amount) implements Media<JsonObject> {

    @Override
    public JsonObject serialize() {
        return new JsonObject(new LinkedHashMap<>(2))
            .put(BankProtocol.TX_CREDIT_LIMIT, this.account.creditLimit())
            .put(BankProtocol.TX_BALANCE, this.account.balance());
    }
}
