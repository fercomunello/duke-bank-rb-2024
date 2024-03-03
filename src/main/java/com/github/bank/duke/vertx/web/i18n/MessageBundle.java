package com.github.bank.duke.vertx.web.i18n;

import io.quarkus.logging.Log;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.mutiny.core.http.HttpHeaders;
import jakarta.enterprise.inject.spi.CDI;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public interface MessageBundle {

    Locale LOCALE_EN_US = Locale.ENGLISH;
    Locale LOCALE_PT_BR = new Locale.Builder().setLanguage("pt").setRegion("BR").build();

    ResourceBundle defaultResourceBundle();
    ResourceBundle brazilianResourceBundle();

    default ResourceBundle resourceBundle() {
        final var request = CDI.current().select(HttpServerRequest.class).get();
        final var acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (acceptLanguage == null) {
            return defaultResourceBundle();
        }
        final String[] locale = acceptLanguage.split("-");
        if (locale.length == 2) {
            if (LOCALE_PT_BR.getLanguage().equalsIgnoreCase(locale[0])
                && LOCALE_PT_BR.getCountry().equalsIgnoreCase(locale[1])) {
                return brazilianResourceBundle();
            }
        }
        return defaultResourceBundle();
    }

    default String get(final String key) {
        try {
            return resourceBundle().getString(key);
        } catch (final MissingResourceException ex) {
            Log.errorf("Cannot find an internationalized message with \"%s\" key.", key);
            return "[null]";
        }
    }
}