package com.github.bank.duke.vertx.sql.function;

import com.github.bank.duke.vertx.sql.SQLConnection;
import io.smallrye.mutiny.Uni;

@FunctionalInterface
public interface UniTask<T> {
    
    Uni<T> execute(SQLConnection connection);
}
