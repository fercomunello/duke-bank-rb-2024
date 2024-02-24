package com.github.bank.duke.business.entity;

public record BankAccount(long creditLimit, long balance) {

    public boolean isCreditLimitExceeded() {
        return this.balance < -this.creditLimit;
    }
}