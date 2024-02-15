package com.github.bank.duke.entity.data;

import com.github.bank.duke.entity.BankAccount;

public interface BankAccounts {

    BankAccount
      ACCOUNT   = new BankAccount(1L, Shinobi.NARUTO_GENIN, 1000 * 100), // $1,000,000
      ACCOUNT_2 = new BankAccount(2L, Shinobi.KAKASHI_JONIN, 80000 * 100), // $80,000,000
      ACCOUNT_3 = new BankAccount(3L, Shinobi.JIRAYA_SENNIN, 10000 * 100), // $10,000,000
      ACCOUNT_4 = new BankAccount(4L, Shinobi.TSUNADE_HOKAGE, 100000 * 100), // $100,000.00
      ACCOUNT_5 = new BankAccount(5L, Shinobi.GAI_JOUNIN, 5000 * 100); // $5,000.00

    String CREDIT_TX_REASON = "#Credit TX",
           DEBIT_TX_REASON = "#Debit TX";
}
