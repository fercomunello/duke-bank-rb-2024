package com.github.bank.duke.http;

import com.github.bank.duke.http.media.Media;
import org.jetbrains.annotations.Nullable;

public sealed interface Response<T extends Media>
    permits Response.Error, Response.Ok, Response.UnprocessableEntity {

    record Ok<T extends Media>(T entity) implements Response<T> {
        public int statusCode() { return 200; }
    }

    record UnprocessableEntity<T extends Media>(T entity) implements Response<T> {
        public UnprocessableEntity() {
            this(null);
        }
        public int statusCode() { return 422; }
    }

    record Error<T extends Media>() implements Response<T> {
        public int statusCode() { return 500; }
    }

    int statusCode();

    @Nullable
    default T entity() {
        return null;
    }
}