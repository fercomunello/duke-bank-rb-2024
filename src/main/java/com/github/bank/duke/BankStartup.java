package com.github.bank.duke;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.CommandLineArguments;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Startup
@Singleton
final class BankStartup {

    @Inject
    BankSchema schema;

    @Inject
    @CommandLineArguments
    String[] arguments;

    public void onStartup(@Observes final StartupEvent event) {
        final var environment = LaunchMode.current();
        switch (environment) {
            case DEVELOPMENT, TEST -> {
                for (final var arg : this.arguments) {
                    if (arg.equals("regenerate-bank-schema")) {
                        this.schema.regenerate();
                        break;
                    }
                }
            }
        }
    }
}