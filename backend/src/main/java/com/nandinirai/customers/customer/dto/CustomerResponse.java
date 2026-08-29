package com.nandinirai.customers.customer.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import com.nandinirai.customers.customer.Customer;

/**
 * Outbound representation. Age is computed per request rather than stored, and
 * is included because every consumer of this data would otherwise recompute it.
 */
public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        int age,
        Instant createdAt) {

    public static CustomerResponse from(Customer customer, LocalDate today) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getDateOfBirth(),
                Period.between(customer.getDateOfBirth(), today).getYears(),
                customer.getCreatedAt());
    }
}
