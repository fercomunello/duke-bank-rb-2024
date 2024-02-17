package com.github.bank.duke.vertx.sql;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.spi.CDI;

import java.util.function.Function;

public interface Database {

    <T> Uni<T> withTransaction(final Function<SQLConnection, Uni<T>> function);

    <T> Uni<T> withSession(final Function<SQLConnection, Uni<T>> function);

    static Database instance() {
        return CDI.current()
            .select(Database.class)
            .get();
    }
}