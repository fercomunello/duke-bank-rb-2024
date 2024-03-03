package com.github.bank.duke.vertx.web.validation.constraints;

import com.github.bank.duke.vertx.web.CachedEntry;
import com.github.bank.duke.vertx.web.i18n.MessageBundle;
import com.github.bank.duke.vertx.web.validation.Validation;
import com.github.bank.duke.vertx.web.validation.ValidationState;

public final class TextLengthMax implements Validation {

    private final CharSequence text;
    private final int max;
    private final CachedEntry key;
    private final boolean required;

    public TextLengthMax(final CharSequence text,
                         final int max) {
        this(text, max, () -> "text");
    }

    public TextLengthMax(final CharSequence text,
                         final int max,
                         final CachedEntry key) {
        this(text, max, key, false);
    }

    public TextLengthMax(final CharSequence text,
                         final int max,
                         final CachedEntry key,
                         final boolean required) {
        this.text = text;
        this.max = max;
        this.key = key;
        this.required = required;
    }

    @Override
    public ValidationState validate(final MessageBundle messages) {
        if (this.required && (this.text == null || this.text.isEmpty())) {
            return ValidationState.invalid(this.key, () ->
                messages.get("$.cannot.be.empty").formatted(this.key.label())
            );
        }
        if (this.text != null && this.text.length() > this.max) {
            return ValidationState.invalid(this.key, () ->
                messages.get("$.cannot.have.more.than.$.characters")
                    .formatted(this.key.label(), this.max)
            );
        }
        return ValidationState.ok(this.key);
    }
}