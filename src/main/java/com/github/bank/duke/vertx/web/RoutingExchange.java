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
            .onItem().transform(content ->
                content.map(Media::serialize)
                    .map(Object::toString)
                    .orElse(Media.NO_CONTENT)
            );
    }

    private <T> Optional<Media<T>> responseWithContent(final Response<T> response) {
        final Media<T> media = prepareResponse(response).entity();
        return Optional.ofNullable(media);
    }

    private <T> Response<T> prepareResponse(final Response<T> response) {
        this.routingContext.response().setStatusCode(response.status().statusCode());
        return response;
    }
}