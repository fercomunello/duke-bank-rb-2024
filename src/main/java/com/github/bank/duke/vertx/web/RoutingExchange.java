package com.github.bank.duke.vertx.web;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

public final class RoutingExchange {

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

    private <T> Optional<T> responseWithContent(final Response<T> response) {
        final T entity = prepareResponse(response).entity();
        return Optional.ofNullable(entity);
    }

    private <T> Response<T> prepareResponse(final Response<T> response) {
        this.routingContext.response().setStatusCode(response.status().statusCode());
        return response;
    }
}