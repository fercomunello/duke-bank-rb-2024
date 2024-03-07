package com.github.bank.duke;

import com.github.bank.duke.business.control.BankProtocol;
import com.github.bank.duke.business.control.BankStatement;
import com.github.bank.duke.business.entity.BankAccount;
import com.github.bank.duke.vertx.sql.Database;
import com.github.bank.duke.vertx.sql.Dialect;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Singleton
final class BankWarmup {

    @Inject
    Bank bank;

    @Inject
    BankStatement bankStatement;

    @Inject
    Database database;

    public void await() {
        Log.info("Warming up...");
        final var startTime = Instant.now();

        final List<Long> accountIds = IntStream.range(0, 100)
            .mapToObj(_ -> this.bank.createAccount(new BankAccount(0, 0))
                .await().indefinitely()).toList();

        try (var virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            final List<Callable<Object>> callables = new ArrayList<>();

            for (final var id : accountIds) {
                @Language(value = "json")
                final var creditPayload = """
                    { "valor": 1000,
                      "tipo" : "C",
                      "descricao" : "Deposit (+)" 
                    }""";
                callables.add(() -> processBankTx(id, creditPayload));
            }
            virtualThreadExecutor.invokeAll(callables);
            callables.clear();

            for (final var id : accountIds) {
                @Language(value = "json")
                final var debitPayload = """
                    { "valor": 500,
                      "tipo" : "D",
                      "descricao" : "Withdrawal (-)" 
                    }""";
                callables.add(() -> processBankTx(id, debitPayload));
            }
            virtualThreadExecutor.invokeAll(callables);
            callables.clear();

            for (final var id : accountIds) {
                callables.add(() -> fetchBankStatement(id));
            }
            virtualThreadExecutor.invokeAll(callables);
        } catch (final InterruptedException ex) {
            throw new RuntimeException(ex);
        }

        @Language(value = Dialect.PSQL)
        final var sql = """
            DELETE FROM bank_transactions WHERE TRUE;
            DELETE FROM bank_accounts WHERE id > 5;
            ALTER SEQUENCE bank_transactions_id_seq RESTART;
            ALTER SEQUENCE bank_accounts_id_seq RESTART WITH 6;""";

        for (final var statement : sql.split(";")) {
            this.database.execute(statement).await().indefinitely();
        }

        Log.infof("Runtime warmup took ~ %dms",
            Duration.between(startTime, Instant.now()).toMillis());
    }

    private HttpResponse<?> processBankTx(final Long id, final String payload) {
        try (var client = HttpClient.newBuilder().build()) {
            final var request = HttpRequest.newBuilder()
                .uri(new URI(BankProtocol.BANK_TRANSACTIONS_URI.replace(":id", id.toString())))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final URISyntaxException | IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private HttpResponse<?> fetchBankStatement(final Long id) {
        try (var client = HttpClient.newBuilder().build()) {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(BankProtocol.BANK_STATEMENT_URI.replace(":id", id.toString())))
                .GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final URISyntaxException | IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
