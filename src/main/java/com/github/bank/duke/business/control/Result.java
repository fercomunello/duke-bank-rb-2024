package com.github.bank.duke.business.control;

import java.util.function.Supplier;

public interface Result<T> {

    T value();

    record Success<T>(T value) implements Result<T> {}

    record Failure<T>(T value, Supplier<String> reason) implements Result<T> {

        public Failure(T value) {
            this(value, null);
        }
    }
}