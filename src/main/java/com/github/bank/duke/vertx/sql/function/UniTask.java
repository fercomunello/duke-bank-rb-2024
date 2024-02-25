package com.github.bank.duke.vertx.sql.function;

import com.github.bank.duke.vertx.sql.SqlConnection;
import io.smallrye.mutiny.Uni;

@FunctionalInterface
public interface UniTask<T> {
    
    Uni<T> execute(SqlConnection connection);
}
