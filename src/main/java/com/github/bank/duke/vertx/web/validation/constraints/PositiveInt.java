package com.github.bank.duke.vertx.web.validation.constraints;

import com.github.bank.duke.vertx.web.CachedEntry;
import com.github.bank.duke.vertx.web.i18n.MessageBundle;
import com.github.bank.duke.vertx.web.validation.Validation;
import com.github.bank.duke.vertx.web.validation.ValidationState;

public final class PositiveInt implements Validation {

    private final Number number;
    private final CachedEntry key;

    public PositiveInt(final Number number, final CachedEntry key) {
        this.number = number;
        this.key = key;
    }

    @Override
    public ValidationState validate(final MessageBundle messages) {
        if (this.number == null) {
            return ValidationState.invalid(this.key, () ->
                messages.get("$.must.be.provided").formatted(this.key.label())
            );
        }
        final Boolean result = switch (this.number) {
            case Short value -> value > 0;
            case Integer value -> value > 0;
            case Long value -> value > 0;
            default -> null;
        };
        if (Boolean.TRUE.equals(result)) {
            return ValidationState.ok(this.key);
        } else if (Boolean.FALSE.equals(result)) {
            return ValidationState.invalid(this.key, () ->
                messages.get("$.cannot.be.negative").formatted(this.key.label())
            );
        } else {
            return ValidationState.badData(this.key, () ->
                messages.get("$.must.be.an.integer.number").formatted(this.key.label())
            );
        }
    }
}