package com.nandinirai.customers.customer;

/**
 * Raised when a record with the same first name, last name and date of birth
 * already exists. See {@code CustomerService} for why this is treated as a
 * conflict rather than silently accepted.
 */
public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String firstName, String lastName) {
        super("A customer named %s %s with that date of birth already exists".formatted(firstName, lastName));
    }
}
