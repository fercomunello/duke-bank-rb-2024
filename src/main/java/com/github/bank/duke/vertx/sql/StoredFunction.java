package com.github.bank.duke.vertx.sql;

import com.github.bank.duke.vertx.sql.function.UniTask;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.spi.CDI;

public abstract class StoredFunction<Output> {

    protected <T> Uni<T> withTransaction(final UniTask<T> task) {
        final Pool pool = CDI.current()
            .select(Pool.class)
            .get();
        return pool.withTransaction(connection -> task.execute(connection, pool));
    }

    public abstract Uni<Output> execute();

}

