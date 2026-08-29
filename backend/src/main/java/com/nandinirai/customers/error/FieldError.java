package com.nandinirai.customers.error;

/**
 * One rejected input, addressed by the same name the client sent. The UI binds
 * these straight onto form controls, so the field name is part of the contract.
 */
public record FieldError(String field, String message) {
}
