package com.github.bank.duke.vertx.sql.vendor;

import com.github.bank.duke.vertx.sql.Pool;
import com.github.bank.duke.vertx.sql.function.RowMapper;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowIterator;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.sqlclient.TransactionPropagation;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.function.Function;

@Unremovable
@Default @Singleton
public final class Postgres implements Pool {

    @Inject
    PgPool pool;

    @Override
    public <T> Uni<T> withTransaction(final Function<SqlConnection, Uni<T>> function) {
        return this.pool.withTransaction(TransactionPropagation.CONTEXT, function);
    }

    @Override
    public <T> Uni<T> withSession(final Function<SqlConnection, Uni<T>> function) {
        return this.pool.withConnection(function);
    }

    @Override
    public <T> Uni<Optional<T>> executeQuery(
        final SqlConnection connection,
        final String sql, final io.vertx.sqlclient.Tuple tuple, final RowMapper<T> mapper)
    {
        return connection.preparedQuery(sql)
            .execute(new io.vertx.mutiny.sqlclient.Tuple(tuple))
            .onItem().transform((rowSet) -> {
                final RowIterator<Row> iterator = rowSet.iterator();
                if (iterator.hasNext()) {
                    final Row row = iterator.next();
                    return Optional.of(mapper.map(row));
                } else {
                    return Optional.empty();
                }
            }
        );
    }
}
