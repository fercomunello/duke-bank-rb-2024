package com.github.bank.duke.vertx.http.media;

public interface Json extends Media {

    io.vertx.core.json.JsonObject asJson();

    @Override
    default String serialize() {
        return asJson().toString();
    }
}