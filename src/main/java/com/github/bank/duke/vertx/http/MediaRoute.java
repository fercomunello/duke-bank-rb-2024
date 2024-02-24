package com.github.bank.duke.vertx.http;

import com.github.bank.duke.vertx.http.media.Media;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public abstract class MediaRoute {

    protected <T extends Media> Uni<String> process(final Uni<Response<T>> uni,
                                                    final RoutingContext routingContext) {
        return uni.onItem().transform(response -> {
            final T entity = response.entity();
            routingContext.response().setStatusCode(response.statusCode());
            return entity != null ? entity.serialize() : "";
        });
    }
}
