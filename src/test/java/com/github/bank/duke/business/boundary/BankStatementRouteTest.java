package com.github.bank.duke.business.boundary;

import com.github.bank.duke.Bank;
import com.github.bank.duke.business.BankTest;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.vertx.web.HttpStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.github.bank.duke.business.control.BankProtocol.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;

@QuarkusTest
final class BankStatementRouteTest extends BankTest {

    @Inject
    Bank bank;

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    @DisplayName("HTTP GET -> 200: Account Bank Statement")
    void testGetAccountBankStatement() {
        final long accountId = this.bank.createAccount(100_000 * 100, 0)
            .await().indefinitely();

        this.bank.populateTransactions(accountId, 10)
            .await().indefinitely();

        given()
            .contentType(ContentType.JSON)
            .get(BANK_STATEMENT_URI.replace(":id", Long.toString(accountId)))
            .prettyPeek().then()
            .statusCode(HttpStatus.OK.statusCode())
            .body(STR."\{BALANCE}.\{TOTAL}", greaterThan(0))
            .body(STR."\{BALANCE}.\{STATEMENT_TIME}", matchesPattern(DATE_TIME_PATTERN))
            .body(STR."\{BALANCE}.\{CREDIT_LIMIT}", equalTo(100_000 * 100))
            .body(STR."\{LAST_TXS}.size()", is(10))
            .body(STR."\{LAST_TXS}[0].\{TX_AMOUNT}", greaterThan(0))
            .body(STR."\{LAST_TXS}[0].\{TX_TYPE}", in(Arrays.stream(TransactionType.values())
                .map(TransactionType::symbol).toList()))
            .body(STR."\{LAST_TXS}[0].\{TX_DESCRIPTION}", not(empty()))
            .body(STR."\{LAST_TXS}[0].\{TX_ISSUED_AT_TIME}", matchesPattern(DATE_TIME_PATTERN));
    }

    @RunOnVertxContext(runOnEventLoop = false)
    @DisplayName("HTTP GET -> 404: Account Not Found")
    void testAccountNotFound() {
        given()
            .contentType(ContentType.JSON)
            .get(BANK_STATEMENT_URI.replace(":id", Long.toString(999)))
            .prettyPeek().then()
            .statusCode(HttpStatus.NOT_FOUND.statusCode())
            .body(empty());
    }
}
