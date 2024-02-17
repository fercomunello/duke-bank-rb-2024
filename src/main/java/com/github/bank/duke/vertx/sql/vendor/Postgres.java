package com.github.bank.duke.vertx.sql.vendor;

import com.github.bank.duke.vertx.sql.SQLConnection;
import com.github.bank.duke.vertx.sql.Database;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.sqlclient.TransactionPropagation;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.function.Function;

@Unremovable
@Default @Singleton
public final class Postgres implements Database {

    @Inject
    PgPool pool;

    @Override
    public <T> Uni<T> withTransaction(final Function<SQLConnection, Uni<T>> function) {
        return this.pool.withTransaction(TransactionPropagation.CONTEXT,
            delegate -> function.apply(new SQLConnection(delegate))
        );
    }

    @Override
    public <T> Uni<T> withSession(final Function<SQLConnection, Uni<T>> function) {
        return this.pool.withConnection(delegate ->
            function.apply(new SQLConnection(delegate))
        );
    }
}
