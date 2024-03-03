package com.github.bank.duke.vertx.web.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Named;

import java.util.ResourceBundle;

import static java.util.ResourceBundle.*;

@ApplicationScoped
@Named(value = "vld")
public final class ValidationMessages implements MessageBundle {

    private static final ResourceBundle BUNDLE_PT_BR = getBundle("i18n/validations", LOCALE_PT_BR);
    private static final ResourceBundle BUNDLE_EN_US = getBundle("i18n/validations", LOCALE_EN_US);

    public static MessageBundle instance() {
        return CDI.current().select(ValidationMessages.class).get();
    }

    @Override
    public ResourceBundle defaultResourceBundle() {
        return BUNDLE_EN_US;
    }

    @Override
    public ResourceBundle brazilianResourceBundle() {
        return BUNDLE_PT_BR;
    }
}