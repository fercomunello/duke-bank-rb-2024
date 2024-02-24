package com.github.bank.duke.http.media;

import io.vertx.core.json.JsonObject;

public interface Json extends Media {

    JsonObject asJson();

    @Override
    default String serialize() {
        return asJson().toString();
    }
}
