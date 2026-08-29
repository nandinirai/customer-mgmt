package com.nandinirai.customers.error;

import java.util.Set;
import java.util.TreeSet;

public class InvalidSortException extends RuntimeException {

    private final Set<String> allowed;

    public InvalidSortException(String property, Set<String> allowed) {
        super("Cannot sort by '%s'. Allowed: %s".formatted(property, new TreeSet<>(allowed)));
        this.allowed = new TreeSet<>(allowed);
    }

    public Set<String> getAllowed() {
        return allowed;
    }
}
