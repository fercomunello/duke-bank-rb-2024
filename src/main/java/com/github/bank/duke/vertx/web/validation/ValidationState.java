package com.github.bank.duke.vertx.web.validation;

import com.github.bank.duke.vertx.web.CachedEntry;
import com.github.bank.duke.vertx.web.validation.failure.InvalidEntry;
import com.github.bank.duke.vertx.web.validation.failure.Constraint;
import org.jetbrains.annotations.Nullable;

public final class ValidationState {

    private final CachedEntry key;
    private final ValidationStatus status;
    private final @Nullable Constraint constraint;

    public ValidationState(final CachedEntry key,
                           final ValidationStatus status) {
        this(key, status, Constraint.NONE);
    }

    public ValidationState(final CachedEntry key,
                           final ValidationStatus status,
                           final @Nullable Constraint constraint) {
        this.key = key;
        this.constraint = constraint;
        this.status = status;
    }

    public static ValidationState ok(final CachedEntry key) {
        return new ValidationState(key, ValidationStatus.OK);
    }

    public static ValidationState badData(final CachedEntry key, final Constraint constraint) {
        return new ValidationState(key, ValidationStatus.BAD_DATA, constraint);
    }

    public static ValidationState invalid(final CachedEntry key, final Constraint constraint) {
        return new ValidationState(key, ValidationStatus.INVALID_DATA, constraint);
    }

    public InvalidEntry asInvalidEntry() {
        return new InvalidEntry(this.status, this.key,
            (this.constraint != null ? this.constraint.message() : null));
    }

    public boolean isValid() {
        return this.status == ValidationStatus.OK;
    }

    public String label() {
        return this.key.label();
    }
}