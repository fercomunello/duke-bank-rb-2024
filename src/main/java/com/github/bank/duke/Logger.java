package com.github.bank.duke;

import java.util.function.Supplier;

import static org.jboss.logging.Logger.Level;

public final class Logger {

    private final org.jboss.logging.Logger delegate;

    public Logger(final Class<?> origin) {
        this.delegate = org.jboss.logging.Logger.getLogger(origin);
    }

    public void info(final Supplier<String> message) {
        this.log(Level.INFO, () -> this.delegate.info(message.get()));
    }

    public void info(final Supplier<String> key, final Supplier<String> content) {
        this.log(Level.INFO, () -> this.delegate.info(STR."\{key.get()} \{content.get()}"));
    }

    public void warn(final Supplier<String> message) {
        this.log(Level.WARN, () -> this.delegate.warn(message.get()));
    }

    public void error(final Supplier<String> message) {
        this.log(Level.ERROR, () -> this.delegate.error(message.get()));
    }

    private void log(final Level level, final Runnable runnable) {
        if (this.delegate.isEnabled(level)) {
            runnable.run();
        }
    }
}