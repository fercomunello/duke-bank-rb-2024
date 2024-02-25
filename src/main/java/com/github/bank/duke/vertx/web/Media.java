package com.github.bank.duke.vertx.web;

public interface Media<T> {

    String NO_CONTENT = "";

    T serialize();
}
