package com.github.bank.duke.vertx.sql;

import com.github.bank.duke.vertx.sql.function.RowMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;

import java.util.Optional;
import java.util.function.Function;

public interface Pool {

    <T> Uni<T> withTransaction(final Function<SqlConnection, Uni<T>> function);

    <T> Uni<T> withSession(final Function<SqlConnection, Uni<T>> function);

    <T> Uni<Optional<T>> executeQuery(SqlConnection connection,
                                      String sql,
                                      Tuple tuple,
                                      RowMapper<T> mapper);
}