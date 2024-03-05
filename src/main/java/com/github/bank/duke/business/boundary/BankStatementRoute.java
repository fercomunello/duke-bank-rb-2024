package com.github.bank.duke.business.boundary;

import com.github.bank.duke.business.control.BankProtocol;
import com.github.bank.duke.business.control.BankStatement;
import com.github.bank.duke.vertx.web.Response;
import com.github.bank.duke.vertx.web.RoutingExchange;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;

import static io.quarkus.vertx.web.ReactiveRoutes.APPLICATION_JSON;
import static io.quarkus.vertx.web.Route.HttpMethod;

@RouteBase(produces = APPLICATION_JSON)
final class BankStatementRoute {

    @Inject
    BankStatement bankStatement;

    @Route(methods = HttpMethod.GET, path = BankProtocol.BANK_STATEMENT_URI)
    public Uni<JsonObject> get(final @Param("id") Long accountId, final RoutingContext context) {
        return new RoutingExchange(context).endWithJson(
            this.bankStatement.aggregateBankStatements(accountId).map(json ->
                json.isEmpty() ? new Response.NotFound<>() : new Response.Ok<>(json)
            )
        );
    }
}
