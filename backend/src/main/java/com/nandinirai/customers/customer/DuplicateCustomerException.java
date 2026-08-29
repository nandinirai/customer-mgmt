package com.nandinirai.customers.customer;

public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String firstName, String lastName) {
        super("A customer named %s %s with that date of birth already exists".formatted(firstName, lastName));
    }
}
