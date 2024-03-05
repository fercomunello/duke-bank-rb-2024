package com.github.bank.duke.business.boundary;

import com.github.bank.duke.business.Bank;
import com.github.bank.duke.business.BankTest;
import com.github.bank.duke.business.entity.TransactionType;
import com.github.bank.duke.vertx.web.HttpStatus;
import com.github.bank.duke.vertx.web.Media;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.bank.duke.business.control.BankProtocol.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
final class BankTransactionRouteTest extends BankTest {

    @Inject
    Bank bank;

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    @DisplayName("HTTP POST -> 200: Credit Transaction")
    void testPostCreditTransaction() {
        final var accountId = this.bank.createAccount(0, 0)
            .await().indefinitely();

        given()
            .contentType(ContentType.JSON)
            .body(STR."""
                {
                  "\{TX_AMOUNT}": \{(1_000 * 100)},
                  "\{TX_TYPE}": "\{TransactionType.CREDIT.symbol()}",
                  "\{TX_DESCRIPTION}": "++☕☕"
                }
                """)
            .post(BANK_TRANSACTIONS_URI.replace(":id", accountId.toString()))
            .prettyPeek().then()
            .statusCode(HttpStatus.OK.statusCode())
            .body(CREDIT_LIMIT, is(0))
            .body(BALANCE, equalTo(1_000 * 100));
    }

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    @DisplayName("HTTP POST -> 200: Debit Transaction")
    void testPostDebitTransaction() {
        final var accountId = this.bank.createAccount(20_000 * 100, 2_000 * 100)
            .await().indefinitely();

        given()
            .contentType(ContentType.JSON)
            .body(STR."""
                {
                  "\{TX_AMOUNT}": \{(12_000 * 100)},
                  "\{TX_TYPE}": "\{TransactionType.DEBIT.symbol()}",
                  "\{TX_DESCRIPTION}": "--☕"
                }
                """)
            .post(BANK_TRANSACTIONS_URI.replace(":id", accountId.toString()))
            .prettyPeek().then()
            .statusCode(HttpStatus.OK.statusCode())
            .body(CREDIT_LIMIT, is(20_000 * 100))
            .body(BALANCE, equalTo(-(10_000 * 100)));
    }

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    @DisplayName("HTTP POST -> 422: Debit Transaction that exceeds account credit limit")
    void testPostDebitTransactionWithoutCreditLimit() {
        final var accountId = this.bank.createAccount(30_000 * 100, -(30_000 * 100))
            .await().indefinitely();

        given()
            .contentType(ContentType.JSON)
            .body(STR."""
                {
                  "\{TX_AMOUNT}": \{(2_000 * 100)},
                  "\{TX_TYPE}": "\{TransactionType.DEBIT.symbol()}",
                  "\{TX_DESCRIPTION}": "--☕"
                }
                """)
            .post(BANK_TRANSACTIONS_URI.replace(":id", accountId.toString()))
            .prettyPeek().then()
            .statusCode(HttpStatus.UNPROCESSABLE_CONTENT.statusCode())
            .body(is(Media.NO_CONTENT));
    }
}