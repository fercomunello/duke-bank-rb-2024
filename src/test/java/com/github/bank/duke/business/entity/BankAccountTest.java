package com.github.bank.duke.business.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class BankAccountTest {

    @Test
    @DisplayName("Positive bank account with sufficient credit limit ")
    void testAccountWithCreditLimit() {
        var account = new BankAccount(500 * 100, -(500 * 100));
        Assertions.assertFalse(account.isCreditLimitExceeded());

        account = new BankAccount(0, +100);
        Assertions.assertFalse(account.isCreditLimitExceeded());
    }

    @Test
    @DisplayName("Negative bank account without credit limit available")
    void testAccountWithoutCreditLimit() {
        var account = new BankAccount(500 * 100, -(600 * 100));
        Assertions.assertTrue(account.isCreditLimitExceeded());

        account = new BankAccount(0, -100);
        Assertions.assertTrue(account.isCreditLimitExceeded());
    }
}
