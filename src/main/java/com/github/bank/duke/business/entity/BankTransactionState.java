package com.github.bank.duke.business.entity;

import com.github.bank.duke.business.control.BankProtocol;
import com.github.bank.duke.http.media.Json;
import io.vertx.core.json.JsonObject;

import java.util.LinkedHashMap;

public record BankTransactionState(BankAccount account,
                                   TransactionType type,
                                   long amount) implements Json {

    @Override
    public JsonObject asJson() {
        return new JsonObject(new LinkedHashMap<>(2))
            .put(BankProtocol.CREDIT_LIMIT, this.account.creditLimit())
            .put(BankProtocol.BALANCE, this.account.balance());
    }
}
