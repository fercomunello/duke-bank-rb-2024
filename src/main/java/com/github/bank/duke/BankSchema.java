package com.github.bank.duke;

import com.github.bank.duke.vertx.sql.Database;
import io.vertx.sqlclient.DatabaseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * <p>Run database schema scripts automatically, for TEST and DEV profiles only.</p> <br>
 *
 * <p>TEST: Triggers schema regeneration before starting to run the test suite.</p>
 *
 * <p>DEV: Triggers schema regeneration between dev-mod reloads:</p>
 * $ mvn clean quarkus:dev -Dquarkus.args=regenerate-bank-schema <br><br>
 *
 * Note: For development, you could simply run ./postgres-local.sh script too as it is more flexible.<br><br>
 */
@Singleton
public final class BankSchema {

    private static final Logger LOG = Logger.getLogger(BankSchema.class);
    private static final String SQL_SCHEMA_PATH = "postgresql/bank-schema.sql";

    @Inject
    Database database;

    public void regenerate() {
        final URL bankSchemaResource = Thread.currentThread()
            .getContextClassLoader().getResource(SQL_SCHEMA_PATH);

        if (bankSchemaResource == null) {
            LOG.errorf("Cannot find %s script.", SQL_SCHEMA_PATH);
            return;
        }

        final byte[] bytes;
        try (InputStream stream = bankSchemaResource.openStream()) {
            bytes = stream.readAllBytes();
        } catch (final IOException ex) {
            LOG.errorf(ex, "Cannot read %s file from classpath.", SQL_SCHEMA_PATH);
            return;
        }

        if (bytes.length > 0) {
            final var sql = new String(bytes);
            this.database.execute(sql)
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