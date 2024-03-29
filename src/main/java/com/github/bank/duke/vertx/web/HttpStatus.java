package com.github.bank.duke.vertx.web;

public enum HttpStatus {

    OK (200),
    BAD_REQUEST (400),
    NOT_FOUND (404),
    UNPROCESSABLE_CONTENT (422),
    INTERNAL_SERVER_ERROR (500);

    private final short statusCode;

    HttpStatus(final int statusCode) {
        this.statusCode = (short) statusCode;
    }

    public short statusCode() {
        return this.statusCode;
    }
}