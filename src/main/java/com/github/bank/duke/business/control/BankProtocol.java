package com.github.bank.duke.business.control;

/**
 * Messages and Naming conventions
 * for RB-2024-Q1 - Rinha de Backend 2024.
 */
public final class BankProtocol {

    // HTTP Routes
    public static final String
        BANK_TRANSACTIONS_URI = "/clientes/:id/transacoes"
    ;

    // Transaction Types / Symbols
    public static final char
        CREDIT_SYMBOL = 'c',
        DEBIT_SYMBOL = 'd'
    ;

    // Payload Fields
    public static final String
        CREDIT_LIMIT = "limite",
        BALANCE = "saldo",
        AMOUNT = "valor",
        TX_TYPE = "tipo",
        DESCRIPTION = "descricao"
    ;

    private BankProtocol() {}
}
