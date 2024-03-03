package com.github.bank.duke.vertx.web.validation.failure;

import java.util.List;

public sealed interface ValidationError {

    record BadData(List<InvalidEntry> entries) implements ValidationError {}
    record InvalidData(List<InvalidEntry> entries) implements ValidationError {}
    record Other(List<InvalidEntry> entries) implements ValidationError {}

    @FunctionalInterface
    interface Producer {
        ValidationError createFrom(List<InvalidEntry> entries);
    }
}