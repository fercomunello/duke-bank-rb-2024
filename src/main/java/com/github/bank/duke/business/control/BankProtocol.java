package com.github.bank.duke.business.control;

import io.vertx.core.json.JsonObject;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Messages and Naming conventions
 * for RB-2024-Q1 - Rinha de Backend 2024.
 */
public final class BankProtocol {

    // HTTP Routes
    public static final String
        BANK_TRANSACTIONS_URI = "/clientes/:id/transacoes",
        BANK_STATEMENT_URI = "/clientes/:id/extrato";

    // Payload Fields
    public static final String
        BALANCE = "saldo",
        CREDIT_LIMIT = "limite",
        TOTAL = "total",
        TX_AMOUNT = "valor",
        TX_TYPE = "tipo",
        TX_DESCRIPTION = "descricao",
        TX_ISSUED_AT_TIME = "realizada_em",
        STATEMENT_TIME = "data_extrato",
        LAST_TXS = "ultimas_transacoes"
    ;

    // Date & Time Pattern: 2024-01-17T02:34:41.217753Z
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    public static final Pattern DATE_TIME_PATTERN = Pattern.compile(
        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{6}Z");

    public static final JsonObject EMPTY_JSON = new JsonObject();

    private BankProtocol() {}
}