package com.github.bank.duke.vertx.web;

import com.github.bank.duke.vertx.web.validation.failure.ValidationError;
import com.github.bank.duke.vertx.web.validation.failure.ValidationException;
import io.smallrye.mutiny.Uni;
import org.jetbrains.annotations.Nullable;

public sealed interface Response<T>
    permits Response.BadRequest, Response.Error, Response.Ok, Response.UnprocessableEntity {

    record Ok<T>(T entity) implements Response<T> {
        public HttpStatus status() { return HttpStatus.OK; }
    }

    record UnprocessableEntity<T>(T entity) implements Response<T> {
        public UnprocessableEntity() {
            this(null);
        }
        public HttpStatus status() { return HttpStatus.UNPROCESSABLE_CONTENT; }
    }

    record BadRequest<T>() implements Response<T> {
        public HttpStatus status() { return HttpStatus.BAD_REQUEST; }
    }

    record Error<T>() implements Response<T> {
        public HttpStatus status() { return HttpStatus.INTERNAL_SERVER_ERROR; }
    }

    HttpStatus status();

    @Nullable
    default T entity() {
        return null;
    }

    default Response<T> also(final Runnable runnable) {
        runnable.run();
        return this;
    }

    static <T> Uni<Response<T>> invalid(final Throwable throwable) {
        return switch (throwable) {
            case ValidationException exception -> Uni.createFrom().item(
                switch (exception.validationError()) {
                    case ValidationError.InvalidData _ -> new UnprocessableEntity<T>();
                    case ValidationError.BadData _ -> new BadRequest<T>();
                    case ValidationError.Other _ -> new Error<T>();
                }
            );
            case null, default -> {
                final var error = new Error<T>();
                yield Uni.createFrom().item(error);
            }
        };
    }
}