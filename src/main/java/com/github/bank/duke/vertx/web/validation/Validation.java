package com.github.bank.duke.vertx.web.validation;

import com.github.bank.duke.vertx.web.i18n.MessageBundle;

public interface Validation {

    ValidationState validate(MessageBundle messages);

}