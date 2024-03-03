package com.github.bank.duke.vertx.web.validation.failure;

public final class ValidationException extends Exception {

    private final ValidationError validationError;

    public ValidationException(final ValidationError validationError) {
        this.validationError = validationError;
    }

    public ValidationError validationError() {
        return this.validationError;
    }
}