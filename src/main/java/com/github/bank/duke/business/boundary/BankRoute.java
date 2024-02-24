package com.github.bank.duke.business.boundary;

import com.github.bank.duke.http.Response;
import com.github.bank.duke.http.media.Media;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public abstract class BankRoute {

    <T extends Media> Uni<String> process(final Uni<Response<T>> uni,
                                          final RoutingContext routingContext) {
        return uni.onItem().transform(bankResponse -> {
            final T entity = bankResponse.entity();
            routingContext.response().setStatusCode(bankResponse.statusCode());
            return entity != null ? entity.serialize() : "";
        });
    }
}
