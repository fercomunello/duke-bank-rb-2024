package com.github.bank.duke.vertx.sql.function;

import com.github.bank.duke.vertx.sql.Pool;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;

@FunctionalInterface
public interface UniTask<T> {
    
    Uni<T> execute(SqlConnection connection, Pool pool);
}
