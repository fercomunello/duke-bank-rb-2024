package com.github.bank.duke.business.boundary;

import com.github.bank.duke.BankSchema;
import com.github.bank.duke.business.control.Bank;
import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.vertx.web.HttpStatus;
import com.github.bank.duke.vertx.web.Media;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.bank.duke.business.control.BankProtocol.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
final class BankTransactionRouteTest {

    @Inject
    Bank bank;

    @Inject
    BankSchema bankSchema;

    @AfterEach
    void afterEach() {
        this.bankSchema.regenerate();
    }

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    @DisplayName("HTTP POST -> 200: Credit Transaction")
    public void testPostCreditTransaction() {
        final var accountId = createAccount(0, 0);

        given()
            .contentType(ContentType.JSON)
            .body(STR."""
                {
                  "\{TX_AMOUNT}": \{(1_000 * 100)},
                  "\{TX_TYPE}": "\{CREDIT_SYMBOL}",
                  "\{TX_DESCRIPTION}": "++☕☕"
                }
                """)
            .post(BANK_TRANSACTIONS_URI.replace(":id", accountId.toString())).then()
            .statusCode(HttpStatus.OK.statusCode())
            .body(TX_CREDIT_LIMIT, is(0))
            .body(TX_BALANCE, equalTo(1_000 * 100));
    }

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    @DisplayName("HTTP POST -> 200: Debit Transaction")
    public void testPostDebitTransaction() {
        final var accountId = createAccount(20_000 * 100, 2_000 * 100);

        given()
            .contentType(ContentType.JSON)
            .body(STR."""
                {
                  "\{TX_AMOUNT}": \{(12_000 * 100)},
                  "\{TX_TYPE}": "\{DEBIT_SYMBOL}",
                  "\{TX_DESCRIPTION}": "--☕"
                }
                """)
            .post(BANK_TRANSACTIONS_URI.replace(":id", accountId.toString())).then()
            .statusCode(HttpStatus.OK.statusCode())
            .body(TX_CREDIT_LIMIT, is(20_000 * 100))
            .body(TX_BALANCE, equalTo(-(10_000 * 100)));
    }

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    @DisplayName("HTTP POST -> 422: Debit Transaction that exceeds account credit limit")
    public void testPostDebitTransactionWithoutCreditLimit() {
        final var accountId = createAccount(30_000 * 100, -(30_000 * 100));

        given()
            .contentType(ContentType.JSON)
            .body(STR."""
                {
                  "\{TX_AMOUNT}": \{(2_000 * 100)},
                  "\{TX_TYPE}": "\{DEBIT_SYMBOL}",
                  "\{TX_DESCRIPTION}": "--☕"
                }
                """)
            .post(BANK_TRANSACTIONS_URI.replace(":id", accountId.toString())).then()
            .statusCode(HttpStatus.UNPROCESSABLE_CONTENT.statusCode())
            .body(is(Media.NO_CONTENT));
    }

    private Long createAccount(final long creditLimit, final long balance) {
        return this.bank.createAccount(new BankAccount(creditLimit, balance)).await().indefinitely();
    }
}
