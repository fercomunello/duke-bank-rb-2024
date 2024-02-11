package com.github.fercomunello.api;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.CommandLineArguments;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.sqlclient.DatabaseException;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * <p>Run database schema scripts automatically, for development only.</p> <br>
 *
 * <p>Regenerate the schema on every reload:</p>
 * $ mvn clean quarkus:dev -Dquarkus.args=regenerate-bank-schema <br><br>
 */
@Singleton
final class BankSchema {

    private static final Logger LOG = Logger.getLogger(BankSchema.class);

    private static final String SQL_SCHEMA_PATH = "postgresql/bank-schema.sql",
                                REGENERATE_DB_SCHEMA = "regenerate-bank-schema";

    @Inject
    PgPool pool;

    @Inject
    @CommandLineArguments
    String[] arguments;

    public void onStart(@Observes final StartupEvent event) {
        if (LaunchMode.current() != LaunchMode.DEVELOPMENT) {
            return;
        }
        for (final var arg : this.arguments) {
            if (!arg.equals(REGENERATE_DB_SCHEMA)) {
                return;
            }
        }

        final URL bankSchemaResource = Thread.currentThread()
            .getContextClassLoader().getResource(SQL_SCHEMA_PATH);

        if (bankSchemaResource == null) {
            LOG.warnf("Cannot find %s script.", SQL_SCHEMA_PATH);
            return;
        }

        final byte[] bytes;
        try (InputStream stream = bankSchemaResource.openStream()) {
            bytes = stream.readAllBytes();
        } catch (final IOException ex) {
            LOG.warnf(ex, "Cannot read %s file from classpath.", SQL_SCHEMA_PATH);
            return;
        }

        if (bytes.length > 0) {
            final var sql = new String(bytes);
            this.pool.query(sql).execute()
                .invoke(() -> LOG.info("Database schema regenerated succesfully."))
                .onFailure().invoke(throwable -> {
                    if (throwable instanceof DatabaseException ex) {
                        LOG.errorf("SQL Error: %s", ex.getMessage());
                    } else {
                        LOG.error("An error occurred while creating the schema.", throwable);
                    }
                }).await().indefinitely();
        }
    }
}
