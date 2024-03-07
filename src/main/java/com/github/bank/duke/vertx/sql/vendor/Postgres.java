package com.github.bank.duke.vertx.sql.vendor;

import com.github.bank.duke.vertx.sql.SqlConnection;
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
    public <T> Uni<T> withTransaction(final Function<SqlConnection, Uni<T>> function) {
        return this.pool.withTransaction(TransactionPropagation.NONE,
            delegate -> function.apply(new SqlConnection(delegate))
        );
    }

    @Override
    public <T> Uni<T> withConnection(final Function<SqlConnection, Uni<T>> function) {
        return this.pool.withConnection(delegate ->
            function.apply(new SqlConnection(delegate))
        );
    }

    @Override
    public Uni<Void> execute(final String sql) {
        return this.pool.withConnection(connection -> connection.query(sql).execute()).replaceWithVoid();
    }
}
