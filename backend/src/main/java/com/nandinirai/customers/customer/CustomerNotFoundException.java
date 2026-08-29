package com.nandinirai.customers.customer;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

    private final UUID id;

    public CustomerNotFoundException(UUID id) {
        super("No customer with id " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
