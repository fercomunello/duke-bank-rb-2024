package com.github.bank.duke.vertx.web.validation.failure;

import com.github.bank.duke.vertx.web.CachedEntry;
import com.github.bank.duke.vertx.web.validation.ValidationStatus;
import org.jetbrains.annotations.Nullable;

public record InvalidEntry(ValidationStatus status,
                           CachedEntry key,
                           @Nullable String reason) {}