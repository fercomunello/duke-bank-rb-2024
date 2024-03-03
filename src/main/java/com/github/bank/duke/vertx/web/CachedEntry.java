package com.github.bank.duke.vertx.web;

import com.github.bank.duke.vertx.web.i18n.Messages;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public interface CachedEntry {

    ConcurrentMap<String, String> LABELS = new ConcurrentHashMap<>();

    String name();

    default String label() {
        final String key, underscoreName = this.name();
        key = LABELS.computeIfAbsent(underscoreName,
            k -> k.replace('_', '.').toLowerCase()
        );
        return Messages.instance().get(key);
    }
}