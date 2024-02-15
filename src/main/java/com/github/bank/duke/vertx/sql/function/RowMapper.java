package com.github.bank.duke.vertx.sql.function;

import io.vertx.mutiny.sqlclient.Row;

@FunctionalInterface
public interface RowMapper<T> {
    T map(Row row);
}
