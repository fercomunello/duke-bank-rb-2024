package com.github.bank.duke.vertx.web.validation.constraints;

import com.github.bank.duke.vertx.web.CachedEntry;
import com.github.bank.duke.vertx.web.i18n.MessageBundle;
import com.github.bank.duke.vertx.web.validation.Validation;
import com.github.bank.duke.vertx.web.validation.ValidationState;
import com.github.bank.duke.vertx.web.validation.failure.Constraint;
import org.jetbrains.annotations.Nullable;

public final class NotNull implements Validation {

    private final Object target;
    private final CachedEntry key;
    private final @Nullable Constraint constraint;

    public NotNull(final Object target, final CachedEntry key) {
        this(target, key, Constraint.NONE);
    }

    public NotNull(final Object target,
                   final CachedEntry key,
                   final @Nullable Constraint constraint) {
        this.target = target;
        this.key = key;
        this.constraint = constraint;
    }

    @Override
    public ValidationState validate(final MessageBundle messages) {
        if (this.target == null) {
            return ValidationState.invalid(this.key, this.constraint == null
                ? () -> messages.get("$.must.be.provided").formatted(this.key.label())
                : this.constraint
            );
        }
        return ValidationState.ok(this.key);
    }

    @FunctionalInterface
    public interface Handler {
        ValidationState invalid(CachedEntry key, MessageBundle messages);
    }
}