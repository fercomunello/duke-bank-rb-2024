package com.github.bank.duke.vertx.web;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;
import java.util.function.Supplier;

public final class RoutingExchange {

    private static final JsonObject EMPTY_JSON_OBJECT = new JsonObject();
    private static final Supplier<JsonObject> EMPTY_JSON_SUPPLIER = () -> EMPTY_JSON_OBJECT;

    private final RoutingContext routingContext;

    public RoutingExchange(final RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    public <T> Uni<String> end(final Uni<Response<T>> responseUni) {
        return responseUni.onItem().transform(this::responseWithContent)
            .onItem().transform(optional ->
                optional.map(entity -> entity instanceof Media<?> media ? media.serialize() : entity)
                    .orElse(Media.NO_CONTENT).toString()
            );
    }

    public Uni<JsonObject> endWithJson(final Uni<Response<JsonObject>> responseUni) {
        return this.endWith(responseUni, EMPTY_JSON_SUPPLIER);
    }

    private <T> Uni<T> endWith(final Uni<Response<T>> responseUni, final Supplier<T> defaultValue) {
        return responseUni.onItem().transform(response -> prepareResponse(response).entity())
            .replaceIfNullWith(defaultValue);
    }

    private <T> Optional<T> responseWithContent(final Response<T> response) {
        final T entity = prepareResponse(response).entity();
        return Optional.ofNullable(entity);
    }

    private <T> Response<T> prepareResponse(final Response<T> response) {
        this.routingContext.response().setStatusCode(response.status().statusCode());
        return response;
    }
}