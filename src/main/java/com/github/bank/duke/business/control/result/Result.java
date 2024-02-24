package com.github.bank.duke.business.control.result;

public interface Result<T> {

    default T entity() {
        return null;
    }
}
