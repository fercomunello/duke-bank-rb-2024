package com.github.bank.duke.vertx.web.validation.failure;

@FunctionalInterface
public interface Constraint {

    Constraint NONE = null;

    String message();
}