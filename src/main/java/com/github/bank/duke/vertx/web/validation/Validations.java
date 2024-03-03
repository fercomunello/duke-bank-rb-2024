package com.github.bank.duke.vertx.web.validation;

import com.github.bank.duke.vertx.web.i18n.ValidationMessages;
import com.github.bank.duke.vertx.web.validation.failure.InvalidEntry;
import com.github.bank.duke.vertx.web.validation.failure.ValidationError;
import com.github.bank.duke.vertx.web.validation.failure.ValidationException;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Validations {

    private static final Logger LOG = Logger.getLogger(Validations.class);
    private static final ValidationFailure NO_FAILURE_CALLBACK = null;

    private final Validation[] validations;

    public Validations(final Validation... validations) {
        this.validations = validations;
    }

    public <T> Uni<T> eval(final Class<?> originClass, final ValidItem<T> item) {
        return eval(originClass, item, NO_FAILURE_CALLBACK);
    }

    public <T> Uni<T> eval(final Class<?> originClass, final ValidItem<T> item,
                           final @Nullable ValidationFailure failure) {
        final List<ValidationState> results = new ArrayList<>(this.validations.length);
        for (final var val : this.validations) {
            results.add(val.validate(ValidationMessages.instance()));
        }
        return Uni.createFrom().emitter(emitter -> {
            if (results.stream().allMatch(ValidationState::isValid)) {
                emitter.complete(item.produce());
            } else {
                final var entries = results.stream()
                    .filter(entry -> !entry.isValid())
                    .map(ValidationState::asInvalidEntry).toList();

                final var error = entries.stream().anyMatch(entry -> entry.status() == ValidationStatus.BAD_DATA)
                        ? new ValidationError.BadData(entries) : new ValidationError.InvalidData(entries);

                emitter.fail(new ValidationException(error));

                emitter.onTermination(() -> {
                    doLog(originClass, entries);
                    if (failure != null) {
                        failure.accept(entries);
                    }
                });
            }
        });
    }

    private void doLog(final Class<?> originClass, final List<InvalidEntry> entries) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(STR."""
               [Failed validations :: \{originClass.getSimpleName()}.java]
               \{entries.stream()
                    .map(entry -> STR."\s\{entry.status()} => \{entry.reason()}")
                    .collect(Collectors.joining("\n"))}
               """);
        }
    }

    @FunctionalInterface
    public interface ValidItem<T> {
        T produce();
    }

    @FunctionalInterface
    public interface ValidationFailure {
        void accept(List<InvalidEntry> invalidEntries);
    }
}