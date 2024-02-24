package com.github.bank.duke.business.boundary;

import com.github.bank.duke.business.control.BankProtocol;
import com.github.bank.duke.business.control.BankTransaction;
import com.github.bank.duke.business.entity.BankTransactionState;
import com.github.bank.duke.vertx.http.MediaRoute;
import com.github.bank.duke.vertx.http.Response;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Singleton;

import static com.github.bank.duke.business.control.BankTransactionResult.*;
import static io.quarkus.vertx.web.ReactiveRoutes.APPLICATION_JSON;
import static io.quarkus.vertx.web.Route.HttpMethod;

@Singleton
@RouteBase(produces = APPLICATION_JSON, consumes = APPLICATION_JSON)
final class BankTransactionRoute extends MediaRoute {

    @Route(methods = HttpMethod.POST, path = BankProtocol.BANK_TRANSACTIONS_URI)
    public Uni<String> post(final RoutingContext routingContext,
                            final @Param("id") Long accountId,
                            final @Body JsonObject payload) {
        return process(new BankTransaction(accountId, payload)
            .execute().onItem().transform(result ->
                switch (result) {
                    case TransactionPerformed tx -> {
                        final BankTransactionState txState = tx.transactionState();
                        yield new Response.Ok<>(txState);
                    }
                    case AccountCreditExceeded _ -> new Response.UnprocessableEntity<>();
                    case TransactionFailure _ -> new Response.Error<>();
                }
            ), routingContext);
    }
}
