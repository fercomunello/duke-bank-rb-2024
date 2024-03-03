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

    // Payload Fields
    public static final String
        TX_CREDIT_LIMIT = "limite",
        TX_BALANCE = "saldo",
        TX_AMOUNT = "valor",
        TX_TYPE = "tipo",
        TX_DESCRIPTION = "descricao"
    ;

    private BankProtocol() {}
}