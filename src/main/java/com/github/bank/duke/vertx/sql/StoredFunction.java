package com.github.bank.duke.vertx.sql;

import com.github.bank.duke.vertx.sql.function.UniTask;
import io.smallrye.mutiny.Uni;

public interface StoredFunction<O> {

    Uni<O> execute();

    default <T> Uni<T> withTransaction(final UniTask<T> task) {
        return Database.instance()
            .withTransaction(task::execute);
    }
}