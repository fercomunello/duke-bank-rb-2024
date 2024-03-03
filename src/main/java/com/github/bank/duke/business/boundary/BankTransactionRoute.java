package com.github.bank.duke.business.boundary;

import com.github.bank.duke.Logger;
import com.github.bank.duke.business.control.BankProtocol;
import com.github.bank.duke.business.control.BankTransaction;
import com.github.bank.duke.vertx.web.Response;
import com.github.bank.duke.vertx.web.RoutingExchange;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static com.github.bank.duke.business.control.BankTransactionResult.*;
import static io.quarkus.vertx.web.ReactiveRoutes.APPLICATION_JSON;
import static io.quarkus.vertx.web.Route.HttpMethod;

@RouteBase(produces = APPLICATION_JSON, consumes = APPLICATION_JSON)
final class BankTransactionRoute {

    private static final Logger LOG = new Logger(BankTransactionRoute.class);

    @Route(methods = HttpMethod.POST, path = BankProtocol.BANK_TRANSACTIONS_URI)
    public Uni<String> post(final @Param("id") Long accountId,
                            final @Body JsonObject payload,
                            final RoutingContext context) {
        return new RoutingExchange(context).end(
            BankTransaction.of(accountId, payload).onItemOrFailure()
                .transformToUni((transaction, failure) -> failure == null ?
                    transaction.perform().onItem().transform(result ->
                        switch (result) {
                            case TransactionPerformed(var state) -> {
                                LOG.info(() -> "[TX Performed]", () ->
                                    STR."\{state.type().description()} of $\{state.amount() / 100}."
                                );
                                yield new Response.Ok<>(state.account());
                            }
                            case AccountCreditExceeded _ ->
                                new Response.UnprocessableEntity<>().also(() ->
                                    LOG.info(() -> "[TX Rejected] Credit limit exceeded, operation rolled back.")
                                );
                            case TransactionFailed _ ->
                                new Response.Error<>().also(() ->
                                    LOG.error(() -> STR."[TX Failed] Operation rolled back. Payload: \{payload}")
                                );
                        })
                    : Response.invalid(failure)
                )
        );
    }
}