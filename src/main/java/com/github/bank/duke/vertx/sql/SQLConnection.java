package com.github.bank.duke.vertx.sql;

import com.github.bank.duke.vertx.sql.function.RowMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowIterator;

import java.util.Optional;

public final class SQLConnection extends io.vertx.mutiny.sqlclient.SqlConnection {

    public SQLConnection(final io.vertx.mutiny.sqlclient.SqlConnection wrapper) {
        super(wrapper.getDelegate());
    }

    public <T> Uni<Optional<T>> executeReturning(final String sql,
                                                 final io.vertx.sqlclient.Tuple tuple,
                                                 final RowMapper<T> mapper)
    {
        return this.preparedQuery(sql)
            .execute(new io.vertx.mutiny.sqlclient.Tuple(tuple))
            .onItem().transform((rowSet) -> {
                final RowIterator<Row> iterator = rowSet.iterator();
                if (iterator.hasNext()) {
                    final Row row = iterator.next();
                    return Optional.of(mapper.map(row));
                } else {
                    return Optional.empty();
                }
            });
    }
}
