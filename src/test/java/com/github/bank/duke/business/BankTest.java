package com.github.bank.duke.business;

import com.github.bank.duke.BankSchema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;

public abstract class BankTest {

    @Inject
    BankSchema bankSchema;

    @AfterEach
    void afterEach() {
        this.bankSchema.regenerate();
    }
}
