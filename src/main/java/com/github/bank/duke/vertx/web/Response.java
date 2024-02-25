package com.github.bank.duke.vertx.web;

import org.jetbrains.annotations.Nullable;

public sealed interface Response<T>
    permits Response.Error, Response.Ok, Response.UnprocessableEntity {

    record Ok<T>(Media<T> entity) implements Response<T> {
        public HttpStatus status() { return HttpStatus.OK; }
    }

    record UnprocessableEntity<T>(Media<T> entity) implements Response<T> {
        public UnprocessableEntity() {
            this(null);
        }
        public HttpStatus status() { return HttpStatus.UNPROCESSABLE_CONTENT; }
    }

    record Error<T>() implements Response<T> {
        public HttpStatus status() { return HttpStatus.INTERNAL_SERVER_ERROR; }
    }

    HttpStatus status();

    @Nullable
    default Media<T> entity() { return null; }
}